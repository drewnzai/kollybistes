package com.kollybistes.core.services;


import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.Transaction;
import com.kollybistes.common.models.User;
import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.util.BitcoinRPC;
import com.kollybistes.core.util.Converter;
import com.kollybistes.core.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BitcoinService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;
    private final ExternalApiHandler externalApiHandler;
    private final TransactionService transactionService;
    private final NotificationProducer notificationProducer;

    @Value("${system.btc.address}")
    private String systemAddress;

    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    public WalletDto createWallet() {
        User user = authService.getCurrentUser();

        if (bitcoinWalletRepository.existsByUser(user)) {
            throw new ResourceAlreadyExistsException("User already has a wallet");
        }

        BitcoinWallet bitcoinWallet = bitcoinRPC.createWallet(user);

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful Bitcoin Wallet Creation")
                        .title("Updated Kollybistes Account Details")
                        .body("You have successfully created a Bitcoin wallet," +
                                " tied to your account with address: "
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

    @Cacheable(
            value = "bitcoin-balances",
            key = "T(org.springframework.security.core.context.SecurityContextHolder)" +
                    ".getContext().getAuthentication().getName()"
    )
    public WalletDto getWalletBalance() {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        bitcoinWallet.setBalance(bitcoinRPC.getTrustedAddressBalance(user.getUsername()));
        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public TransactionDto calculateTransactionDetails(String recipientAddress, BigDecimal amountBtc) {

        if(!ValidationUtil.isValidBitcoinAddress(recipientAddress)){
            throw new IllegalFormatException("Invalid Bitcoin address: " + recipientAddress);
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        BigDecimal updatedBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        bitcoinWallet.setBalance(updatedBalanceBtc);
        BigInteger updatedBalanceSats = Converter.convertBtcToSats(bitcoinWallet.getBalance());

        BigInteger amountSat = Converter.convertBtcToSats(amountBtc);
        BigDecimal transactionFeeBtc = amountBtc.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger transactionFeeSat = Converter.convertBtcToSats(transactionFeeBtc);

        BigInteger feeRate = externalApiHandler.getRecommendedBitcoinFee(); // in sat/vB
        BigInteger estimatedSize = new BigInteger
                (bitcoinRPC.estimateP2WPKHTransactionSize(1, 2));
        BigInteger networkFeeSat = feeRate.multiply(estimatedSize);

        BigInteger totalCostSats = amountSat.add(transactionFeeSat).add(networkFeeSat);

        if (totalCostSats.compareTo(updatedBalanceSats) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + updatedBalanceBtc.toString()
                    + " BTC");
        }

        BigDecimal totalCostBtc = Converter.convertSatsToBtc(totalCostSats);
        BigDecimal expectedBalanceBtc = updatedBalanceBtc.subtract(totalCostBtc);

        return TransactionDto.builder()
                .amount(amountBtc)
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(
                        transactionFeeBtc,
                        Converter.convertSatsToBtc(networkFeeSat),
                        feeRate
                ))
                .expectedBalance(expectedBalanceBtc)
                .build();
    }

    @Transactional
    public Map<String, String> confirmTransactionToOutsideWallet(TransactionDto transactionDto) {

        if(!ValidationUtil.isValidBitcoinAddress(transactionDto.getRecipientAddress())){
            throw new IllegalFormatException("Invalid Bitcoin address: " +
                    transactionDto.getRecipientAddress());
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        if(bitcoinWallet.isTradingLocked()){
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        BigDecimal updatedBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        BigInteger updatedBalanceSats = Converter.convertBtcToSats(bitcoinWallet.getBalance());

        bitcoinWallet.setTradingLocked(true);
        bitcoinWallet.setBalance(updatedBalanceBtc);
        bitcoinWalletRepository.save(bitcoinWallet);

        BigDecimal totalFeesBtc = transactionDto
                .getFeesDto().getSystemFee().add(transactionDto.getFeesDto().getTransactionFee());
        BigDecimal totalBtc = transactionDto.getAmount().add(totalFeesBtc);
        BigInteger totalSats = Converter.convertBtcToSats(totalBtc);

        if(totalSats.compareTo(updatedBalanceSats) > 0){
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + updatedBalanceBtc.toString()
                    + " BTC");
        }

        BigInteger satvBFeeRate = transactionDto.getFeesDto().getMeasure();

        BigDecimal systemFeeBtc = transactionDto.getFeesDto().getSystemFee();
        BigInteger systemFeeSats = Converter.convertBtcToSats(systemFeeBtc);

        String toSystemHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                systemAddress,
                systemFeeSats,
                satvBFeeRate
        );

        Transaction toSystemTransaction = Transaction.builder()
                .transactionHash(toSystemHash)
                .senderAddress(bitcoinWallet.getAddress())
                .recipientAddress(systemAddress)
                .amount(systemFeeBtc)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactionService.createTransaction(toSystemTransaction, true);

        BigDecimal transactionAmountBtc = transactionDto.getAmount();
        BigInteger transactionAmountSats = Converter.convertBtcToSats(transactionAmountBtc);

        String toRecipientHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                transactionDto.getRecipientAddress(),
                transactionAmountSats,
                satvBFeeRate
        );

        Transaction toRecipientTransaction = Transaction.builder()
                .transactionHash(toRecipientHash)
                .recipientAddress(transactionDto.getRecipientAddress())
                .senderAddress(bitcoinWallet.getAddress())
                .amount(transactionAmountBtc)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactionService.createTransaction(toRecipientTransaction, true);

        BigDecimal finalBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        bitcoinWallet.setBalance(finalBalanceBtc);
        bitcoinWallet.setTradingLocked(false);
        bitcoinWalletRepository.save(bitcoinWallet);

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful Bitcoin Transfer")
                        .body("You have used " + totalBtc.toString()
                        + " BTC to send " + transactionAmountBtc.toString()
                        + " BTC to wallet address: "
                        + transactionDto.getRecipientAddress()
                        + ". Your balance is " + finalBalanceBtc.toString() + " BTC.")
                        .title("Bitcoin Wallet Update")
                        .build()
        );

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("System's TX Hash", toSystemHash);
        txHashes.put("Recipient's TX Hash", toRecipientHash);

        return txHashes;
    }

}
