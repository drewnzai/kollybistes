package com.kollybistes.core.services;


import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.NotificationEmail;
import com.kollybistes.common.models.User;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BitcoinService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;
    private final ExchangeService exchangeService;
    private final NotificationProducer notificationProducer;

    @Value("${system.btc.address}")
    private String systemAddress;
    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if (bitcoinWalletRepository.existsByUser(user)) {
            throw new Exception("User already has a wallet");
        }

        BitcoinWallet bitcoinWallet = bitcoinRPC.createWallet(user);
        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful Bitcoin Wallet Creation")
                        .title("Updated Kollybistes Account Details")
                        .body("You have successfully created a Bitcoin wallet, tied to your account with address: "
                                + bitcoinWallet.getAddress()
                                + " Do not share these details with anyone.")
                        .build()
        );

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(convertSatsToBtc(BigInteger.ZERO))
                .build();
    }

    public WalletDto getWalletBalance() throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        bitcoinWallet.setBalance(bitcoinRPC.getTrustedAddressBalance(user.getUsername()));
        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(convertSatsToBtc(bitcoinWallet.getBalance()))
                .build();
    }

    public TransactionDto calculateTransactionDetails(String recipientAddress, BigDecimal amount) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        BigInteger amountSat = convertBtcToSats(amount);
        BigDecimal transactionFeeBtc = amount.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger transactionFeeSat = convertBtcToSats(transactionFeeBtc);

        BigInteger feeRate = exchangeService.getRecommendedBitcoinFee(); // in sat/vB
        BigInteger estimatedSize = new BigInteger(estimateP2WPKHTransactionSize(1, 2));
        BigInteger networkFeeSat = feeRate.multiply(estimatedSize);

        BigInteger totalCost = amountSat.add(transactionFeeSat).add(networkFeeSat);

        if (totalCost.compareTo(updateBalance(bitcoinWallet)) > 0) {
            throw new Exception("Insufficient balance.");
        }

        return TransactionDto.builder()
                .amount(convertSatsToBtc(amountSat))
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(
                        transactionFeeBtc,
                        convertSatsToBtc(networkFeeSat),
                        feeRate
                ))
                .expectedBalance(convertSatsToBtc(bitcoinWallet.getBalance().subtract(totalCost)))
                .build();
    }

    public Object confirmTransactionToOutsideWallet(TransactionDto transactionDto) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new Exception("User does not have a Bitcoin wallet")
                );

        BigInteger satvBFeeRate = transactionDto.getFeesDto().getMeasure();

        String toSystemHash = bitcoinRPC.sendBitcoinToSystem(
                user.getUsername(),
                systemAddress,
                convertBtcToSats(transactionDto.getFeesDto().getSystemFee()),
                satvBFeeRate
        );

        String toRecipientHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                transactionDto.getRecipientAddress(),
                convertBtcToSats(transactionDto.getAmount()),
                satvBFeeRate
        );

        bitcoinWallet.setBalance(updateBalance(bitcoinWallet));
        bitcoinWalletRepository.save(bitcoinWallet);

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("System TX Hash", toSystemHash);
        txHashes.put("Recipient TX Hash", toRecipientHash);

        return txHashes;
    }

    private BigInteger updateBalance(BitcoinWallet bitcoinWallet){
        BigInteger updated = bitcoinRPC
                .getTrustedAddressBalance(bitcoinWallet
                        .getUser()
                        .getUsername());

        bitcoinWallet.setBalance(updated);

        bitcoinWalletRepository.save(bitcoinWallet);

        return updated;
    }

    private String estimateP2WPKHTransactionSize(int inputCount, int outputCount) {
        final int TX_OVERHEAD = 11;             // Version + locktime + input/output counts
        final int P2WPKH_INPUT_SIZE = 68;       // P2WPKH input size in vbytes
        final int P2WPKH_OUTPUT_SIZE = 31;      // P2WPKH output size in vbytes

        int totalSize = TX_OVERHEAD
                + (inputCount * P2WPKH_INPUT_SIZE)
                + (outputCount * P2WPKH_OUTPUT_SIZE);

        return String.valueOf(totalSize); // size in vbytes
    }

    private BigInteger convertBtcToSats(BigDecimal btc) {
        return btc.multiply(BigDecimal.valueOf(100_000_000L)).toBigInteger();
    }

    private BigDecimal convertSatsToBtc(BigInteger sats) {
        return new BigDecimal(sats).divide(BigDecimal.valueOf(100_000_000L));
    }

}
