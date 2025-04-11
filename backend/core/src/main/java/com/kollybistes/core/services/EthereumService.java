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
import org.web3j.utils.Convert;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class EthereumService {

    private final Web3j web3j;
    private final AuthService authService;
    private final EthereumRepository ethereumRepository;
    private static final String KEYSTORE_PATH = "/home/andrew/Ethereum/private/keystore/";
    private final NotificationProducer notificationProducer;
    private final ExchangeService exchangeService;
    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");
    @Value("${system.eth.address}")
    private String systemAddress;

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if(ethereumRepository.existsByUser(user)){
            throw new Exception("User already has an Ethereum account");
        }

        ECKeyPair keyPair = Keys.createEcKeyPair();
        WalletFile walletFile = Wallet.createStandard(user.getPassword(), keyPair);
        WalletUtils
                .generateWalletFile(user.getPassword(), keyPair, new File(KEYSTORE_PATH), false);

        EthereumWallet ethereumWallet = new EthereumWallet();

        ethereumWallet.setUser(user);
        ethereumWallet.setBalance(BigDecimal.valueOf(0l));
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

        EthereumWallet ethWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        // 15% transaction fee to be paid to system wallet
        BigDecimal transactionFeeAmount = amountInEth.multiply(TRANSACTION_FEE_PERCENT);

        // Get recommended gas price from ExchangeService (*2 for two transactions)
        BigDecimal recommendedGasPrice = exchangeService.getRecommendedEthereumGasFee()
                .multiply(BigDecimal.valueOf(2L));
        BigDecimal gasLimit = BigDecimal.valueOf(21000L); // standard for ETH transfer
        BigDecimal gasCost = recommendedGasPrice.multiply(gasLimit);

        BigDecimal finalAmount = amountInEth.add(transactionFeeAmount).add(gasCost);

        if (finalAmount.compareTo(ethWallet.getBalance()) > 0) {
            throw new Exception("User does not have the necessary balance");
        }

        return TransactionDto.builder()
                .amount(amountInEth)
                .recipientAddress(recipientAddress)
                .feesDto(new FeesDto(transactionFeeAmount, gasCost))
                .expectedBalance(ethWallet.getBalance().subtract(finalAmount))
                .build();
    }

    public BigDecimal getBalance(String address) throws Exception {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return Convert.fromWei(new BigDecimal(balance.getBalance()), Convert.Unit.ETHER);
    }

}
