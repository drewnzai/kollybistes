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

        if(bitcoinWalletRepository.existsByUser(user)){
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
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public WalletDto getWalletBalance() throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                        .orElseThrow(
                                () -> {
                                    return new Exception("User does not have a Bitcoin wallet");
                                }
                        );
        bitcoinWallet.setBalance(
                bitcoinRPC.getTrustedAddressBalance(user.getUsername()));

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public TransactionDto sendBitcoinToOutsideWallet(String recipientAddress, BigDecimal amount) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> {
                            return new Exception("User does not have a Bitcoin wallet");
                        }
                );

        BigDecimal transactionAmount = amount.multiply(TRANSACTION_FEE_PERCENT); //Bitcoin to be sent to the system
        BigInteger satvBFeeRate = exchangeService.getRecommendedBitcoinFee();
        BigInteger totalFeeSat = satvBFeeRate.multiply(
                new BigInteger("2")) //Gets the total amount of transaction fees
                // for both transactions
                .multiply(
                        new BigInteger
                                (estimateP2WPKHTransactionSize(1,2)) // Get the
                        // estimated size of the transaction (around 141 vB)
                        //and multipy it by the fee rate (sat/vB) to get the fee size (sat)
                );

        // 1 BTC = 100,000,000 sat
        BigDecimal totalFeeBTC = new BigDecimal(totalFeeSat).divide(BigDecimal.valueOf(100000000L));

        BigDecimal finalAmount = amount.add(transactionAmount).add(totalFeeBTC);

        if(finalAmount.compareTo(updateBalance(bitcoinWallet)) >= 0){
            throw new Exception("User does not have the necessary balance");
        }

        return TransactionDto.builder()
                .amount(amount)
                .recipientAddress(recipientAddress)
                .feesDto(
                        new FeesDto(transactionAmount, totalFeeBTC, satvBFeeRate)
                )
                .expectedBalance(bitcoinWallet.getBalance().subtract(finalAmount))
                .build();
    }

    public Object confirmTransaction(TransactionDto transactionDto) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> {
                            return new Exception("User does not have a Bitcoin wallet");
                        }
                );

        BigInteger satvBFeeRate = transactionDto.getFeesDto().getMeasure();

        String toSystemHash = bitcoinRPC.sendBitcoinToSystem(
                user.getUsername(),
                systemAddress,
                transactionDto.getFeesDto().getSystemFee(),
                satvBFeeRate
        );

        String toRecipientHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                transactionDto.getRecipientAddress(),
                transactionDto.getAmount(),
                satvBFeeRate
        );

        bitcoinWallet.setBalance(updateBalance(bitcoinWallet));
        bitcoinWalletRepository.save(bitcoinWallet);

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("System TX Hash", toSystemHash);
        txHashes.put("Recipient TX Hash", toRecipientHash);

        return txHashes;
    }

    private BigDecimal updateBalance(BitcoinWallet bitcoinWallet){
        BigDecimal updated = bitcoinRPC
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


}
