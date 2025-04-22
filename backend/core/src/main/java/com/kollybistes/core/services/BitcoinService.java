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
    private final APIHandler apiHandler;
    private final TransactionService transactionService;
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
                .balance(BigDecimal.ZERO)
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
                .balance(bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance()))
                .build();
    }

    public TransactionDto calculateTransactionDetails(String recipientAddress, BigDecimal amount) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        bitcoinWallet.setBalance(bitcoinRPC.updateBalance(bitcoinWallet));

        BigInteger amountSat = bitcoinRPC.convertBtcToSats(amount);
        BigDecimal transactionFeeBtc = amount.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger transactionFeeSat = bitcoinRPC.convertBtcToSats(transactionFeeBtc);

        BigInteger feeRate = apiHandler.getRecommendedBitcoinFee(); // in sat/vB
        BigInteger estimatedSize = new BigInteger
                (bitcoinRPC.estimateP2WPKHTransactionSize(1, 2));
        BigInteger networkFeeSat = feeRate.multiply(estimatedSize);

        BigInteger totalCost = amountSat.add(transactionFeeSat).add(networkFeeSat);

        if (totalCost.compareTo(bitcoinWallet.getBalance()) > 0) {
            throw new Exception("Insufficient balance. You have "
                    + bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance()).toString());
        }

        return TransactionDto.builder()
                .amount(bitcoinRPC.convertSatsToBtc(amountSat))
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(
                        transactionFeeBtc,
                        bitcoinRPC.convertSatsToBtc(networkFeeSat),
                        feeRate
                ))
                .expectedBalance(bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance().subtract(totalCost)))
                .build();
    }

    public Object confirmTransactionToOutsideWallet(TransactionDto transactionDto) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new Exception("User does not have a Bitcoin wallet")
                );

        if(bitcoinWallet.isTradingLocked()){
            throw new Exception("Wallet is currently in a transaction, try again later");
        }

        bitcoinWallet.setTradingLocked(true);
        bitcoinWallet.setBalance(bitcoinRPC.updateBalance(bitcoinWallet));
        bitcoinWalletRepository.save(bitcoinWallet);

        BigDecimal totalFeesBtc = transactionDto
                .getFeesDto().getSystemFee().add(transactionDto.getFeesDto().getTransactionFee());
        BigDecimal totalBtc = transactionDto.getAmount().add(totalFeesBtc);
        BigInteger totalSats = bitcoinRPC.convertBtcToSats(totalBtc);

        if(totalSats.compareTo(bitcoinWallet.getBalance()) > 0){
            throw new Exception("Insufficient balance. You have "
                    + bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance()).toString());
        }

        BigInteger satvBFeeRate = transactionDto.getFeesDto().getMeasure();

        BigInteger systemFeeSats = bitcoinRPC.convertBtcToSats(transactionDto.getFeesDto().getSystemFee());

        String toSystemHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                systemAddress,
                systemFeeSats,
                satvBFeeRate
        );

        transactionService.saveTransaction(
                bitcoinWallet.getAddress(),
                systemAddress,
                systemFeeSats,
                toSystemHash
        );

        BigInteger transactionAmountSats = bitcoinRPC.convertBtcToSats(transactionDto.getAmount());

        String toRecipientHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                transactionDto.getRecipientAddress(),
                transactionAmountSats,
                satvBFeeRate
        );

        transactionService.saveTransaction(
                bitcoinWallet.getAddress(),
                transactionDto.getRecipientAddress(),
                transactionAmountSats,
                toRecipientHash
        );

        bitcoinWallet.setBalance(bitcoinRPC.updateBalance(bitcoinWallet));
        bitcoinWallet.setTradingLocked(false);
        bitcoinWalletRepository.save(bitcoinWallet);

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("System's TX Hash", toSystemHash);
        txHashes.put("Recipient's TX Hash", toRecipientHash);

        return txHashes;
    }

}
