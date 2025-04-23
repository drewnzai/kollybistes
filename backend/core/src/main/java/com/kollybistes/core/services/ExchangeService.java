package com.kollybistes.core.services;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.models.*;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.EthereumRepository;
import com.kollybistes.core.repositories.ExchangeRepository;
import com.kollybistes.core.util.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository exchangeRepository;
    private final APIHandler apiHandler;
    private final AuthService authService;
    private final EthereumRepository ethereumRepository;
    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final BitcoinRPC bitcoinRPC;
    private final Web3j web3j;

    @Value("${system.btc.address}")
    private String systemBtcAddress;

    @Value("${system.eth.address}")
    private String systemEthAddress;

    @Value("${system.eth.chainId}")
    private String ethChainId;

    @Value("${system.eth.keystore.path}")
    private String keystorePath;

    @Value("${system.eth.password}")
    private String keystorePassword;


    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");
    private static final BigDecimal MIN_BTC_AMOUNT = new BigDecimal("0.000012"); // ~1,200 sats
    private static final BigDecimal MIN_ETH_AMOUNT = new BigDecimal("0.00061");  // ~610,000,000,000,000 wei

    public ExchangeDto calculateExchangeDetails(ExchangeDto exchangeDto) throws Exception {
        if(exchangeDto.getExchangeType().equals("BTC_TO_ETH")){
            return calculateBtcToEth(exchangeDto);
        }
        else if(exchangeDto.getExchangeType().equals("ETH_TO_BTC")){
            return calculateEthToBtc(exchangeDto);
        }
        else{
            throw new Exception("Not a valid exchange, check the exchange type");
        }
    }

    public Object confirmExchange(ExchangeDto exchangeDto) throws Exception {
        if(exchangeDto.getExchangeType().equals("BTC_TO_ETH")){
            return confirmBtcToEth(exchangeDto);
        }
        else if(exchangeDto.getExchangeType().equals("ETH_TO_BTC")){
            return confirmEthToBtc(exchangeDto);
        }
        else{
            throw new Exception("Not a valid exchange, check the exchange type");
        }
    }

    private ExchangeDto calculateBtcToEth(ExchangeDto exchangeDto) throws Exception {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_BTC_AMOUNT) < 0) {
            throw new Exception("Exchange amount is too low. Minimum is " + MIN_BTC_AMOUNT + " BTC.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        bitcoinWallet.setBalance(bitcoinRPC.updateBalance(bitcoinWallet));

        BigDecimal amountBtc = exchangeDto.getAmountToExchange();
        BigDecimal systemTransactionFeeBtc = amountBtc.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger amountSat = bitcoinRPC.convertBtcToSats(amountBtc);
        BigInteger systemTransactionFeeSat = bitcoinRPC.convertBtcToSats(systemTransactionFeeBtc);

        BigInteger feeRate = apiHandler.getRecommendedBitcoinFee(); // in sat/vB
        BigInteger estimatedSize = new BigInteger
                (bitcoinRPC.estimateP2WPKHTransactionSize(1, 2));
        BigInteger networkFeeSat = feeRate.multiply(estimatedSize);

        BigDecimal exchangeRate = apiHandler.getBtcToEthExchangeRate();
        BigDecimal expectedReturnEth = amountBtc.multiply(exchangeRate);

        BigInteger totalCost = amountSat.add(systemTransactionFeeSat).add(networkFeeSat);
        BigDecimal expectedBalance = bitcoinRPC
                .convertSatsToBtc(bitcoinWallet.getBalance().subtract(totalCost));

        if (totalCost.compareTo(bitcoinWallet.getBalance()) > 0) {
            throw new Exception("Insufficient balance. You have "
                    + bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance()).toString());
        }

        bitcoinWalletRepository.save(bitcoinWallet);

        return ExchangeDto.builder()
                .amountToExchange(amountBtc)
                .expectedAmountGotten(expectedReturnEth)
                .expectedBalance(expectedBalance)
                .feesDto(
                        new FeesDto(
                                systemTransactionFeeBtc,
                                bitcoinRPC.convertSatsToBtc(networkFeeSat),
                                feeRate
                        )
                )
                .exchangeType(ExchangeType.BTC_TO_ETH.name())
                .rate(exchangeRate)
                .build();
    }

    private ExchangeDto calculateEthToBtc(ExchangeDto exchangeDto) throws Exception{

        if (exchangeDto.getAmountToExchange().compareTo(MIN_ETH_AMOUNT) < 0) {
            throw new Exception("Exchange amount is too low. Minimum is " + MIN_ETH_AMOUNT + " ETH.");
        }

        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        BigInteger balanceWei = getBalance(ethereumWallet.getAddress());

        ethereumWallet.setBalance(balanceWei);
        ethereumRepository.save(ethereumWallet);

        BigDecimal amountInEth = exchangeDto.getAmountToExchange();
        BigInteger amountInWei = convertEthToWei(amountInEth);

        // Calculate 15% system fee (in wei)
        BigDecimal systemFeeEth = amountInEth.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger systemFeeWei = convertEthToWei(systemFeeEth);

        // Estimate gas fees
        BigInteger gasPriceWei = apiHandler.getRecommendedEthereumGasFee(); // in wei
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        BigInteger totalGasFees = gasPriceWei.multiply(gasLimit);

        BigDecimal exchangeRateBtcToEth = apiHandler.getBtcToEthExchangeRate();
        BigDecimal exchangeRateEthToBtc = BigDecimal.ONE.divide
                (exchangeRateBtcToEth, new MathContext(10));
        BigDecimal expectedReturnBtc = exchangeRateEthToBtc.multiply(amountInEth);

        BigInteger totalCost = amountInWei.add(systemFeeWei).add(totalGasFees);

        if (totalCost.compareTo(balanceWei) > 0) {
            throw new Exception("Insufficient balance. You have "
            + convertWeiToEth(balanceWei).toString());
        }

        ethereumRepository.save(ethereumWallet);

        return ExchangeDto.builder()
                .amountToExchange(amountInEth)
                .exchangeType(ExchangeType.ETH_TO_BTC.name())
                .expectedAmountGotten(expectedReturnBtc)
                .expectedBalance(convertWeiToEth(balanceWei.subtract(totalCost)))
                .feesDto(
                        new FeesDto(convertWeiToEth(systemFeeWei),
                                convertWeiToEth(totalGasFees), gasPriceWei)
                )
                .rate(exchangeRateEthToBtc)
                .build();
    }

    private Object confirmBtcToEth(ExchangeDto exchangeDto) throws Exception {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_BTC_AMOUNT) < 0) {
            throw new Exception("Exchange amount is too low. Minimum is " + MIN_BTC_AMOUNT + " BTC.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        if(bitcoinWallet.isTradingLocked()){
            throw new Exception("Wallet is currently in a transaction, try again later");
        }

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        bitcoinWallet.setBalance(bitcoinRPC.updateBalance(bitcoinWallet));

        BigDecimal totalFeesBtc = exchangeDto
                .getFeesDto().getSystemFee().add(exchangeDto.getFeesDto().getTransactionFee());
        BigDecimal totalBtc = exchangeDto.getAmountToExchange().add(totalFeesBtc);
        BigInteger totalSats = bitcoinRPC.convertBtcToSats(totalBtc);

        if(totalSats.compareTo(bitcoinWallet.getBalance()) > 0) {
            throw new Exception("Insufficient balance. You have "
                    + bitcoinRPC.convertSatsToBtc(bitcoinWallet.getBalance()).toString());
        }

        bitcoinWallet.setTradingLocked(true);
        bitcoinWalletRepository.save(bitcoinWallet);

        BigInteger feeRate = exchangeDto.getFeesDto().getMeasure();
        BigInteger systemFeeSats = bitcoinRPC.convertBtcToSats(exchangeDto.getFeesDto().getSystemFee());

        String toSystemHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                systemBtcAddress,
                systemFeeSats,
                feeRate
        );

        bitcoinWallet.setTradingLocked(false);
        bitcoinWalletRepository.save(bitcoinWallet);

        Credentials credentials = loadSystemWalletCredentials();

        BigInteger gasPriceWei = apiHandler.getRecommendedEthereumGasFee();

        String recipientHash = sendEtherToAddress(ethereumWallet.getAddress(),
                exchangeDto.getExpectedAmountGotten(),
                gasPriceWei,
                credentials
                );

        Exchange exchange = Exchange.builder()
                .exchangeType(ExchangeType.BTC_TO_ETH)
                .exchangeRate(exchangeDto.getRate())
                .amountGiven(exchangeDto.getAmountToExchange())
                .amountGotten(exchangeDto.getExpectedAmountGotten())
                .status(ExchangeStatus.COMPLETED)
                .bitcoinWallet(bitcoinWallet)
                .ethereumWallet(ethereumWallet)
                .transactionFee(exchangeDto.getFeesDto().getTransactionFee())
                .systemFee(exchangeDto.getFeesDto().getSystemFee())
                .totalCost(totalBtc)
                .createdAt(Date.from(Instant.now()))
                .build();

        exchangeRepository.save(exchange);

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("Sent BTC", toSystemHash);
        txHashes.put("Received ETH", recipientHash);

        return txHashes;
    }

    private Object confirmEthToBtc(ExchangeDto exchangeDto) throws Exception {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_ETH_AMOUNT) < 0) {
            throw new Exception("Exchange amount is too low. Minimum is " + MIN_ETH_AMOUNT + " ETH.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));

        EthereumWallet ethereumWallet = ethereumRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have an Ethereum wallet"));

        if (ethereumWallet.isTradingLocked()) {
            throw new Exception("Wallet is currently in a transaction, try again later");
        }

        BigInteger balanceWei = getBalance(ethereumWallet.getAddress());

        ethereumWallet.setBalance(balanceWei);
        ethereumWallet.setTradingLocked(true);
        ethereumRepository.save(ethereumWallet);

        BigInteger gasPriceWei = exchangeDto.getFeesDto().getMeasure();

        BigDecimal totalFeesEth = exchangeDto
                .getFeesDto().getSystemFee().add(exchangeDto.getFeesDto().getTransactionFee());
        BigDecimal totalEth = exchangeDto.getAmountToExchange().add(totalFeesEth);
        BigInteger totalWei = convertEthToWei(totalEth);

        if (totalWei.compareTo(balanceWei) > 0) {
            throw new Exception("Insufficient balance. You have "
                    + convertWeiToEth(balanceWei).toString());
        }

        Credentials credentials = Credentials.create(ethereumWallet.getPrivateKey());

        String toSystemHash = sendEtherToAddress(systemEthAddress,
                exchangeDto.getAmountToExchange(),
                gasPriceWei,
                credentials
        );

        ethereumWallet.setTradingLocked(false);
        ethereumRepository.save(ethereumWallet);

        BigInteger feeRate = apiHandler.getRecommendedBitcoinFee();

        BigDecimal amountGottenBtc = exchangeDto.getExpectedAmountGotten();
        BigInteger amountGottenSats = bitcoinRPC.convertBtcToSats(amountGottenBtc);

        String recipientHash = bitcoinRPC.sendBitcoinFromSystem(
                bitcoinWallet.getAddress(),
                amountGottenSats,
                feeRate
        );

        Exchange exchange = Exchange.builder()
                .exchangeType(ExchangeType.ETH_TO_BTC)
                .exchangeRate(exchangeDto.getRate())
                .amountGiven(exchangeDto.getAmountToExchange())
                .amountGotten(exchangeDto.getExpectedAmountGotten())
                .status(ExchangeStatus.COMPLETED)
                .bitcoinWallet(bitcoinWallet)
                .ethereumWallet(ethereumWallet)
                .transactionFee(exchangeDto.getFeesDto().getTransactionFee())
                .systemFee(exchangeDto.getFeesDto().getSystemFee())
                .totalCost(totalEth)
                .createdAt(Date.from(Instant.now()))
                .build();

        exchangeRepository.save(exchange);

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("Sent ETH", toSystemHash);
        txHashes.put("Received BTC", recipientHash);

        return txHashes;
    }

    private String sendEtherToAddress(String recipientAddress,
                                      BigDecimal amountInEther,
                                      BigInteger gasPriceWei,
                                      Credentials credentials) throws Exception {

        BigInteger nonce = web3j.ethGetTransactionCount(
                        credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials,
                Long.parseLong(ethChainId));

        RawTransaction transaction = RawTransaction.createEtherTransaction(
                nonce,
                gasPriceWei,
                BigInteger.valueOf(21000L), // gas limit for standard ETH transfer
                recipientAddress,
                Convert.toWei(amountInEther, Convert.Unit.ETHER).toBigInteger()
        );

        EthSendTransaction response = txManager.signAndSend(transaction);

        if (response.hasError()) {
            throw new Exception("Transaction Error: " + response.getError().getMessage());
        }

        return response.getTransactionHash(); // TXID
    }

    private Credentials loadSystemWalletCredentials() throws Exception {
        return WalletUtils.loadCredentials(keystorePassword, new File(keystorePath));
    }

    private BigInteger getBalance(String address) throws Exception {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return balance.getBalance(); // in wei
    }

    private BigDecimal convertWeiToEth(BigInteger wei) {
        return new BigDecimal(wei).divide(BigDecimal.TEN.pow(18));
    }

    private BigInteger convertEthToWei(BigDecimal eth) {
        return eth.multiply(BigDecimal.TEN.pow(18)).toBigInteger();
    }

}
