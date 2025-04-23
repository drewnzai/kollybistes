package com.kollybistes.core.services;


import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.EthereumRepository;
import com.kollybistes.core.util.Converter;
import com.kollybistes.core.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.tx.RawTransactionManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EthereumService {

    private final Web3j web3j;
    private final AuthService authService;
    private final EthereumRepository ethereumRepository;
    private final TransactionService transactionService;
    private final NotificationProducer notificationProducer;
    private final APIHandler apiHandler;

    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    @Value("${system.eth.address}")
    private String systemAddress;

    @Value("${system.eth.chainId}")
    private String chainId;

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if (ethereumRepository.existsByUser(user)) {
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

        ethereumRepository.save(ethereumWallet);

        return WalletDto.builder()
                .balance(ethereumWallet.getBalance()) // in wei
                .address(ethereumWallet.getAddress())
                .build();
    }

    public TransactionDto calculateTransactionDetails(String recipientAddress, BigDecimal amountInEth)
            throws Exception {

        if (!ValidationUtil.isValidEthereumAddress(recipientAddress)) {
            throw new IllegalFormatException("Invalid Ethereum address: " +
                    recipientAddress);
        }

        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet")
                );


        BigInteger updatedBalanceWei = getBalance(ethereumWallet.getAddress());

        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        ethereumWallet.setBalance(updatedBalanceEth);
        ethereumRepository.save(ethereumWallet);

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

    public Object confirmTransactionToOutsideWallet(TransactionDto transactionDto) throws Exception {

        if (!ValidationUtil.isValidEthereumAddress(transactionDto.getRecipientAddress())) {
            throw new IllegalFormatException("Invalid Ethereum address: " +
                    transactionDto.getRecipientAddress());
        }

        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        if (ethereumWallet.isTradingLocked()) {
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        BigInteger updatedBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        ethereumWallet.setBalance(updatedBalanceEth);
        ethereumWallet.setTradingLocked(true);
        ethereumRepository.save(ethereumWallet);
        
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

        BigInteger systemFeeWei = Converter.convertEthToWei(transactionDto.getFeesDto().getSystemFee());

        RawTransaction toSystem = RawTransaction.createEtherTransaction(
                nonce, gasPriceWei, BigInteger.valueOf(21000L),
                systemAddress, systemFeeWei);

        String toSystemHash = txManager.signAndSend(toSystem).getTransactionHash();

        transactionService.saveTransaction(
                ethereumWallet.getAddress(),
                systemAddress,
                systemFeeWei,
                toSystemHash
        );

        nonce = nonce.add(BigInteger.ONE);

        BigInteger transactionAmountWei = Converter.convertEthToWei(transactionDto.getAmount());

        RawTransaction toRecipient = RawTransaction.createEtherTransaction(
                nonce, gasPriceWei, BigInteger.valueOf(21000L),
                transactionDto.getRecipientAddress(), transactionAmountWei);

        String toRecipientHash = txManager.signAndSend(toRecipient).getTransactionHash();

        transactionService.saveTransaction(
                ethereumWallet.getAddress(),
                transactionDto.getRecipientAddress(),
                transactionAmountWei,
                toRecipientHash
        );

        BigInteger finalBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal finalBalanceEth = Converter.convertWeiToEth(finalBalanceWei);

        ethereumWallet.setTradingLocked(false);
        ethereumWallet.setBalance(finalBalanceEth);
        ethereumRepository.save(ethereumWallet);

        Map<String, String> txDetails = new HashMap<>();
        txDetails.put("System's TX Hash", toSystemHash);
        txDetails.put("Recipient's TX Hash", toRecipientHash);

        return txDetails;
    }

    private BigInteger getBalance(String address) throws Exception {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return balance.getBalance(); // in wei
    }
    
}
