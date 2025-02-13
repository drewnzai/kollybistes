package com.kollybistes.core.services;


import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.repositories.EthereumRepository;
import lombok.RequiredArgsConstructor;
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
    private static final String PASSWORD = "your_strong_password"; // Set a secure password
    private static final String KEYSTORE_PATH = "/home/andrew/Ethereum/private/keystore/";

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if(ethereumRepository.existsByUser(user)){
            throw new Exception("User already has an Ethereum account");
        }

        ECKeyPair keyPair = Keys.createEcKeyPair();
        WalletFile walletFile = Wallet.createStandard(PASSWORD, keyPair);
        WalletUtils
                .generateWalletFile(PASSWORD, keyPair, new File(KEYSTORE_PATH), false);

        EthereumWallet ethereumWallet = new EthereumWallet();

        ethereumWallet.setUser(user);
        ethereumWallet.setBalance(BigDecimal.valueOf(0l));
        ethereumWallet.setPrivateKey(keyPair.getPrivateKey().toString(16));
        ethereumWallet.setPublicKey(keyPair.getPublicKey().toString(16));
        ethereumWallet.setAddress("0x" + walletFile.getAddress());
        ethereumWallet.setCreatedAt(Date.from(Instant.now()));

        ethereumRepository.save(ethereumWallet);

        return WalletDto.builder()
                        .balance(ethereumWallet.getBalance())
                                .address(ethereumWallet.getAddress())
                                        .build();
    }

    public BigDecimal getBalance(String address) throws Exception {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return Convert.fromWei(new BigDecimal(balance.getBalance()), Convert.Unit.ETHER);
    }

    public String loadPrivateKey(String walletFileName) throws Exception {
        Credentials credentials = WalletUtils.loadCredentials(PASSWORD, KEYSTORE_PATH + walletFileName);
        return credentials.getEcKeyPair().getPrivateKey().toString(16);
    }
}
