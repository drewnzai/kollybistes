package com.kollybistes.core.services;


import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.Transaction;
import com.kollybistes.common.models.User;
import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.exceptions.IllegalFormatException;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.EthereumWalletRepository;
import com.kollybistes.core.repositories.TransactionRepository;
import com.kollybistes.core.util.Converter;
import com.kollybistes.core.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.tx.RawTransactionManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EthereumService {

    private final Web3j web3j;
    private final AuthService authService;
    private final EthereumWalletRepository ethereumWalletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationProducer notificationProducer;
    private final APIHandler apiHandler;

    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    @Value("${system.eth.address}")
    private String systemAddress;

    @Value("${system.eth.chainId}")
    private String chainId;

    @SneakyThrows //Do not want outright "throws Exception" in method signature
    public WalletDto createWallet() {
        User user = authService.getCurrentUser();

        if (ethereumWalletRepository.existsByUser(user)) {
            throw new ResourceAlreadyExistsException("User already has an Ethereum account");
        }

        ECKeyPair keyPair = Keys.createEcKeyPair();
        WalletFile walletFile = Wallet.createStandard(user.getPassword(), keyPair);

        EthereumWallet ethereumWallet = new EthereumWallet();
        ethereumWallet.setUser(user);
        ethereumWallet.setBalance(BigDecimal.ZERO); // in wei
        ethereumWallet.setPrivateKey(keyPair.getPrivateKey().toString(16));
        ethereumWallet.setPublicKey(keyPair.getPublicKey().toString(16));
        ethereumWallet.setAddress("0x" + walletFile.getAddress());
        ethereumWallet.setCreatedAt(Date.from(Instant.now()));

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful Ethereum Wallet Creation")
                        .title("Updated Kollybistes Account Details")
                        .body("You have successfully created an Ethereum wallet, tied to your account with address: "
                                + ethereumWallet.getAddress()
                                + " Do not share these details with anyone.")
                        .build()
        );

        ethereumWalletRepository.save(ethereumWallet);

        return WalletDto.builder()
                .balance(ethereumWallet.getBalance()) // in wei
                .address(ethereumWallet.getAddress())
                .build();
    }

    public TransactionDto calculateTransactionDetails(String recipientAddress, BigDecimal amountInEth) {

        if (!ValidationUtil.isValidEthereumAddress(recipientAddress)) {
            throw new IllegalFormatException("Invalid Ethereum address: " +
                    recipientAddress);
        }

        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet")
                );


        BigInteger updatedBalanceWei = getBalance(ethereumWallet.getAddress());

        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        ethereumWallet.setBalance(updatedBalanceEth);
        ethereumWalletRepository.save(ethereumWallet);

        BigInteger amountInWei = Converter.convertEthToWei(amountInEth);

        // Calculate 15% system fee (in wei)
        BigDecimal systemFeeEth = amountInEth.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger systemFeeWei = Converter.convertEthToWei(systemFeeEth);

        // Estimate gas fees
        BigInteger gasPriceWei = apiHandler.getRecommendedEthereumGasFee(); // in wei
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        BigInteger totalGasFees = gasPriceWei.multiply(gasLimit).multiply(BigInteger.TWO);

        BigInteger totalCost = amountInWei.add(systemFeeWei).add(totalGasFees);

        if (totalCost.compareTo(updatedBalanceWei) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + Converter.convertWeiToEth(updatedBalanceWei).toString()
                    + " ETH");
        }

        return TransactionDto.builder()
                .amount(amountInEth)
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(Converter.convertWeiToEth(systemFeeWei),
                        Converter.convertWeiToEth(totalGasFees), gasPriceWei))
                .expectedBalance(Converter.convertWeiToEth(updatedBalanceWei.subtract(totalCost)))
                .build();
    }

    @SneakyThrows
    @Transactional
    public Map<String, String> confirmTransactionToOutsideWallet(TransactionDto transactionDto) {

        if (!ValidationUtil.isValidEthereumAddress(transactionDto.getRecipientAddress())) {
            throw new IllegalFormatException("Invalid Ethereum address: " +
                    transactionDto.getRecipientAddress());
        }

        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet"));

        if (ethereumWallet.isTradingLocked()) {
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        BigInteger updatedBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        ethereumWallet.setBalance(updatedBalanceEth);
        ethereumWallet.setTradingLocked(true);
        ethereumWalletRepository.save(ethereumWallet);
        
        BigInteger gasPriceWei = transactionDto.getFeesDto().getMeasure();

        BigDecimal totalFeesEth = transactionDto
                .getFeesDto().getSystemFee().add(transactionDto.getFeesDto().getTransactionFee());
        BigDecimal totalEth = transactionDto.getAmount().add(totalFeesEth);
        BigInteger totalWei = Converter.convertEthToWei(totalEth);

        if (totalWei.compareTo(updatedBalanceWei) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + Converter.convertWeiToEth(updatedBalanceWei).toString()
                    + " ETH");
        }

        BigInteger nonce = web3j.ethGetTransactionCount(
                ethereumWallet.getAddress(),
                DefaultBlockParameterName.LATEST).send().getTransactionCount();

        Credentials credentials = Credentials.create(ethereumWallet.getPrivateKey());
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, Long.parseLong(chainId));

        BigDecimal systemFeeEth = transactionDto.getFeesDto().getSystemFee();
        BigInteger systemFeeWei = Converter.convertEthToWei(systemFeeEth);

        RawTransaction toSystem = RawTransaction.createEtherTransaction(
                nonce, gasPriceWei, BigInteger.valueOf(21000L),
                systemAddress, systemFeeWei);

        String toSystemHash = txManager.signAndSend(toSystem).getTransactionHash();

        List<Transaction> transactions = new ArrayList<>();

        Transaction toSystemTransaction = Transaction.builder()
                .transactionHash(toSystemHash)
                .senderAddress(ethereumWallet.getAddress())
                .recipientAddress(systemAddress)
                .amount(systemFeeEth)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactions.add(toSystemTransaction);

        nonce = nonce.add(BigInteger.ONE);

        BigDecimal transactionAmountEth = transactionDto.getAmount();
        BigInteger transactionAmountWei = Converter.convertEthToWei(transactionAmountEth);

        RawTransaction toRecipient = RawTransaction.createEtherTransaction(
                nonce, gasPriceWei, BigInteger.valueOf(21000L),
                transactionDto.getRecipientAddress(), transactionAmountWei);

        String toRecipientHash = txManager.signAndSend(toRecipient).getTransactionHash();

        Transaction toRecipientTransaction = Transaction.builder()
                .transactionHash(toRecipientHash)
                .recipientAddress(transactionDto.getRecipientAddress())
                .senderAddress(ethereumWallet.getAddress())
                .amount(transactionAmountEth)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactions.add(toRecipientTransaction);
        transactionRepository.saveAll(transactions);

        BigInteger finalBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal finalBalanceEth = Converter.convertWeiToEth(finalBalanceWei);

        ethereumWallet.setTradingLocked(false);
        ethereumWallet.setBalance(finalBalanceEth);
        ethereumWalletRepository.save(ethereumWallet);

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful Ethereum Transfer")
                        .body("You have used " + totalEth.toString()
                                + " ETH to send " + transactionAmountEth.toString()
                                + " ETH to wallet address: "
                                + transactionDto.getRecipientAddress()
                                + ". Your balance is " + finalBalanceEth.toString() + " ETH.")
                        .title("Ethereum Wallet Update")
                        .build()
        );

        Map<String, String> txDetails = new HashMap<>();
        txDetails.put("System's TX Hash", toSystemHash);
        txDetails.put("Recipient's TX Hash", toRecipientHash);

        return txDetails;
    }

    public WalletDto getWalletBalance() {
        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet"));

        if (ethereumWallet.isTradingLocked()) {
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        String address = ethereumWallet.getAddress();
        BigInteger updatedBalanceWei = getBalance(address);
        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        return WalletDto.builder()
                .address(address)
                .balance(updatedBalanceEth)
                .build();

    }

    @SneakyThrows
    private BigInteger getBalance(String address) {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return balance.getBalance(); // in wei
    }
    
}
