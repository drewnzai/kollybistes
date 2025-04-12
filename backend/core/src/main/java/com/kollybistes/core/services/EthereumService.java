package com.kollybistes.core.services;


import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.NotificationEmail;
import com.kollybistes.common.models.User;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.EthereumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;

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
    private final NotificationProducer notificationProducer;
    private final ExchangeService exchangeService;
    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");
    @Value("${system.eth.address}")
    private String systemAddress;
    @Value("${system.eth.chainId}")
    private String chainId;

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if(ethereumRepository.existsByUser(user)){
            throw new Exception("User already has an Ethereum account");
        }

        ECKeyPair keyPair = Keys.createEcKeyPair();
        WalletFile walletFile = Wallet.createStandard(user.getPassword(), keyPair);

        EthereumWallet ethereumWallet = new EthereumWallet();
        ethereumWallet.setUser(user);
        ethereumWallet.setBalance(BigDecimal.valueOf(0L));
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
                        .balance(ethereumWallet.getBalance())
                                .address(ethereumWallet.getAddress())
                                        .build();
    }

    public TransactionDto sendEthToOutsideWallet(String recipientAddress, BigDecimal amountInEth) throws Exception {
        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        EthGetBalance balanceResponse = web3j.ethGetBalance(ethereumWallet.getAddress(),
                DefaultBlockParameterName.LATEST).send();

        if (balanceResponse.hasError()) {
            throw new Exception("Cannot retrieve ethereum balance from network");
        }

        BigInteger balanceWei = balanceResponse.getBalance();
        BigDecimal balanceEther = Convert.fromWei(balanceWei.toString(), Convert.Unit.ETHER);

        ethereumWallet.setBalance(balanceEther);

        // 15% transaction fee to be paid to system wallet
        BigDecimal transactionFeeAmount = amountInEth.multiply(TRANSACTION_FEE_PERCENT);

        // Get recommended gas price from ExchangeService (*2 for two transactions)
        BigDecimal recommendedGasPrice = exchangeService.getRecommendedEthereumGasFee()
                .multiply(BigDecimal.valueOf(2L));
        BigDecimal gasLimit = BigDecimal.valueOf(21000L); // standard for ETH transfer
        BigDecimal gasCost = recommendedGasPrice.multiply(gasLimit);

        BigDecimal finalAmount = amountInEth.add(transactionFeeAmount).add(gasCost);

        if (finalAmount.compareTo(ethereumWallet.getBalance()) > 0) {
            throw new Exception("User does not have the necessary balance");
        }

        return TransactionDto.builder()
                .amount(amountInEth)
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(transactionFeeAmount, gasCost))
                .expectedBalance(ethereumWallet.getBalance().subtract(finalAmount))
                .build();
    }

    public Object confirmTransaction(TransactionDto transactionDto) throws Exception {
        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        if(ethereumWallet.isTradingLocked()){
           throw new Exception("User's ETH wallet is currently in a transaction");
        }

        ethereumWallet.setTradingLocked(true);
        ethereumRepository.save(ethereumWallet);

        // Division by 42000 (2*21000) to get the gas fee used for individual transactions
        BigDecimal individualGasCost = transactionDto.getFeesDto()
                .getTransactionFee()
                .divide(BigDecimal.valueOf(42000L));

        BigInteger gasPriceWei = Convert.toWei(individualGasCost, Convert.Unit.ETHER).toBigInteger();

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                ethereumWallet.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        Credentials credentials = Credentials.create(ethereumWallet.getPrivateKey());

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, Long.parseLong(chainId));
        RawTransaction toSystem  = RawTransaction.createEtherTransaction(
                nonce,  gasPriceWei,
                BigInteger.valueOf(21000L),
                systemAddress,
                Convert.toWei(transactionDto.getFeesDto().getSystemFee(), Convert.Unit.ETHER)
                .toBigInteger());

        String toSystemHash = txManager.signAndSend(toSystem).getTransactionHash();

        nonce = nonce.add(BigInteger.ONE);

        RawTransaction toRecipient  = RawTransaction.createEtherTransaction(
                nonce, gasPriceWei,
                BigInteger.valueOf(21000L),
                transactionDto.getRecipientAddress(),
                Convert.toWei(transactionDto.getAmount(), Convert.Unit.ETHER).toBigInteger());

        String toRecipientHash = txManager.signAndSend(toRecipient).getTransactionHash();

        ethereumWallet.setTradingLocked(false);
        ethereumWallet.setBalance(getBalance(ethereumWallet.getAddress()));
        ethereumRepository.save(ethereumWallet);

        Map<String, String> txDetails = new HashMap<>();
        txDetails.put("System's TX Hash", toSystemHash);
        txDetails.put("Recipient's TX Hash", toRecipientHash);

        return txDetails;
    }

    public BigDecimal getBalance(String address) throws Exception {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return Convert.fromWei(new BigDecimal(balance.getBalance()), Convert.Unit.ETHER);
    }

}
