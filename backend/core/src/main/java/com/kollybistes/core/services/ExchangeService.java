package com.kollybistes.core.services;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.models.*;
import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.EthereumWalletRepository;
import com.kollybistes.core.repositories.ExchangeRepository;
import com.kollybistes.core.util.BitcoinRPC;
import com.kollybistes.core.util.Converter;
import com.kollybistes.core.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository exchangeRepository;
    private final ExternalApiHandler externalApiHandler;
    private final AuthService authService;
    private final EthereumWalletRepository ethereumWalletRepository;
    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final NotificationProducer notificationProducer;
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

    public ExchangeDto calculateExchangeDetails(ExchangeDto exchangeDto) {
        if(exchangeDto.getExchangeType().equals("BTC_TO_ETH")){
            return calculateBtcToEth(exchangeDto);
        }
        else if(exchangeDto.getExchangeType().equals("ETH_TO_BTC")){
            return calculateEthToBtc(exchangeDto);
        }
        else{
            throw new IllegalFormatException("Not a valid exchange, check the exchange type");
        }
    }

    public Map<String, String> confirmExchange(ExchangeDto exchangeDto) {
        if(exchangeDto.getExchangeType().equals("BTC_TO_ETH")){
            return confirmBtcToEth(exchangeDto);
        }
        else if(exchangeDto.getExchangeType().equals("ETH_TO_BTC")){
            return confirmEthToBtc(exchangeDto);
        }
        else{
            throw new IllegalFormatException("Not a valid exchange, check the exchange type");
        }
    }

    @Cacheable(value = "exchanges",
            key = "#paginationRequest.page + '-' + " +
                    "#paginationRequest.size + '-' + " +
                    "T(org.springframework.security.core.context.SecurityContextHolder)." +
                    "getContext().getAuthentication().getName()")
    public PagingResult<ExchangeDto> getExchanges(PaginationRequest paginationRequest){
        User user = authService.getCurrentUser();

        Pageable pageable = PaginationUtil.getPageable(paginationRequest);
        final Page<Exchange> userExchanges = exchangeRepository.findAllByUser(user, pageable);
        List<ExchangeDto> exchanges = userExchanges
                .stream()
                .map(this::mapExchangeToExchangeDto).toList();

        return new PagingResult<>(
                exchanges,
                userExchanges.getTotalPages(),
                userExchanges.getTotalElements(),
                userExchanges.getSize(),
                userExchanges.getNumber(),
                userExchanges.isEmpty()
        );
    }

    @CachePut(value = "exchanges",
            key = "#paginationRequest.page + '-' + " +
                    "#paginationRequest.size + '-' + " +
                    "T(org.springframework.security.core.context.SecurityContextHolder)." +
                    "getContext().getAuthentication().getName()")
    public PagingResult<ExchangeDto> refreshExchanges(PaginationRequest paginationRequest) {
        return getExchanges(paginationRequest);
    }

    private ExchangeDto calculateBtcToEth(ExchangeDto exchangeDto) {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_BTC_AMOUNT) < 0) {
            throw new InsufficientBalanceException("Exchange amount is too low. Minimum is " + MIN_BTC_AMOUNT + " BTC.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        BigDecimal updatedBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        bitcoinWallet.setBalance(updatedBalanceBtc);
        BigInteger updatedBalanceSats = Converter.convertBtcToSats(bitcoinWallet.getBalance());

        BigDecimal amountBtc = exchangeDto.getAmountToExchange();
        BigDecimal systemTransactionFeeBtc = amountBtc.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger amountSat = Converter.convertBtcToSats(amountBtc);
        BigInteger systemTransactionFeeSat = Converter.convertBtcToSats(systemTransactionFeeBtc);

        BigInteger feeRate = externalApiHandler.getRecommendedBitcoinFee(); // in sat/vB
        BigInteger estimatedSize = new BigInteger
                (bitcoinRPC.estimateP2WPKHTransactionSize(1, 2));
        BigInteger networkFeeSat = feeRate.multiply(estimatedSize);

        BigDecimal exchangeRate = externalApiHandler.getBtcToEthExchangeRate();
        BigDecimal expectedReturnEth = amountBtc.multiply(exchangeRate);

        BigInteger totalCostSats = amountSat.add(systemTransactionFeeSat).add(networkFeeSat);
        BigDecimal totalCostBtc = Converter.convertSatsToBtc(totalCostSats);

        BigDecimal expectedBalance = updatedBalanceBtc.subtract(totalCostBtc);

        if (totalCostSats.compareTo(updatedBalanceSats) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + updatedBalanceBtc.toString()
            + " BTC");
        }

        bitcoinWalletRepository.save(bitcoinWallet);

        return ExchangeDto.builder()
                .amountToExchange(amountBtc)
                .expectedAmountGotten(expectedReturnEth)
                .expectedBalance(expectedBalance)
                .feesDto(
                        new FeesDto(
                                systemTransactionFeeBtc,
                                Converter.convertSatsToBtc(networkFeeSat),
                                feeRate
                        )
                )
                .exchangeType(ExchangeType.BTC_TO_ETH.name())
                .rate(exchangeRate)
                .build();
    }

    private ExchangeDto calculateEthToBtc(ExchangeDto exchangeDto) {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_ETH_AMOUNT) < 0) {
            throw new InsufficientBalanceException
                    ("Exchange amount is too low. Minimum is " + MIN_ETH_AMOUNT + " ETH.");
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

        BigDecimal amountInEth = exchangeDto.getAmountToExchange();
        BigInteger amountInWei = Converter.convertEthToWei(amountInEth);

        // Calculate 15% system fee (in wei)
        BigDecimal systemFeeEth = amountInEth.multiply(TRANSACTION_FEE_PERCENT);
        BigInteger systemFeeWei = Converter.convertEthToWei(systemFeeEth);

        // Estimate gas fees
        BigInteger gasPriceWei = externalApiHandler.getRecommendedEthereumGasFee(); // in wei
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        BigInteger totalGasFees = gasPriceWei.multiply(gasLimit);

        BigDecimal exchangeRateBtcToEth = externalApiHandler.getBtcToEthExchangeRate();
        BigDecimal exchangeRateEthToBtc = BigDecimal.ONE.divide
                (exchangeRateBtcToEth, new MathContext(10));
        BigDecimal expectedReturnBtc = exchangeRateEthToBtc.multiply(amountInEth);

        BigInteger totalCost = amountInWei.add(systemFeeWei).add(totalGasFees);

        if (totalCost.compareTo(updatedBalanceWei) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
            + Converter.convertWeiToEth(updatedBalanceWei).toString()
            + " ETH");
        }

        ethereumWalletRepository.save(ethereumWallet);

        return ExchangeDto.builder()
                .amountToExchange(amountInEth)
                .exchangeType(ExchangeType.ETH_TO_BTC.name())
                .expectedAmountGotten(expectedReturnBtc)
                .expectedBalance(Converter.convertWeiToEth(updatedBalanceWei.subtract(totalCost)))
                .feesDto(
                        new FeesDto(systemFeeEth,
                                Converter.convertWeiToEth(totalGasFees), gasPriceWei)
                )
                .rate(exchangeRateEthToBtc)
                .build();
    }

    @Transactional
    private Map<String, String> confirmBtcToEth(ExchangeDto exchangeDto) {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_BTC_AMOUNT) < 0) {
            throw new InsufficientBalanceException("Exchange amount is too low. Minimum is " + MIN_BTC_AMOUNT + " BTC.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        if(bitcoinWallet.isTradingLocked()){
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet")
                );

        BigDecimal updatedBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        bitcoinWallet.setBalance(updatedBalanceBtc);
        BigInteger updatedBalanceSats = Converter.convertBtcToSats(bitcoinWallet.getBalance());

        BigDecimal totalFeesBtc = exchangeDto
                .getFeesDto().getSystemFee().add(exchangeDto.getFeesDto().getTransactionFee());
        BigDecimal totalBtc = exchangeDto.getAmountToExchange().add(totalFeesBtc);
        BigInteger totalSats = Converter.convertBtcToSats(totalBtc);

        if(totalSats.compareTo(updatedBalanceSats) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + updatedBalanceBtc.toString()
            + " BTC");
        }

        bitcoinWallet.setTradingLocked(true);
        bitcoinWalletRepository.save(bitcoinWallet);

        BigInteger feeRate = exchangeDto.getFeesDto().getMeasure();
        BigInteger systemFeeSats = Converter.convertBtcToSats(exchangeDto.getFeesDto().getSystemFee());

        String toSystemHash = bitcoinRPC.sendBitcoin(
                user.getUsername(),
                systemBtcAddress,
                systemFeeSats,
                feeRate
        );

        BigDecimal finalBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);

        bitcoinWallet.setBalance(finalBalanceBtc);
        bitcoinWallet.setTradingLocked(false);
        bitcoinWalletRepository.save(bitcoinWallet);

        Credentials credentials = loadSystemWalletCredentials();

        BigInteger gasPriceWei = externalApiHandler.getRecommendedEthereumGasFee();

        String recipientHash = sendEtherToAddress(ethereumWallet.getAddress(),
                exchangeDto.getExpectedAmountGotten(),
                gasPriceWei,
                credentials
                );

        BigInteger finalBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal finalBalanceEth = Converter.convertWeiToEth(finalBalanceWei);

        Exchange exchange = Exchange.builder()
                .exchangeType(ExchangeType.BTC_TO_ETH)
                .exchangeRate(exchangeDto.getRate())
                .amountGiven(exchangeDto.getAmountToExchange())
                .amountGotten(exchangeDto.getExpectedAmountGotten())
                .status(ExchangeStatus.COMPLETED)
                .user(user)
                .bitcoinWallet(bitcoinWallet)
                .ethereumWallet(ethereumWallet)
                .transactionFee(exchangeDto.getFeesDto().getTransactionFee())
                .systemFee(exchangeDto.getFeesDto().getSystemFee())
                .totalCost(totalBtc)
                .createdAt(Date.from(Instant.now()))
                .build();

        exchangeRepository.save(exchange);

        refreshExchanges(
                new PaginationRequest(0,
                10,
                "id",
                Sort.Direction.DESC)
        );

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful BTC to ETH Transfer")
                        .body("You have used " + totalBtc.toString()
                                + " BTC and gotten " + exchangeDto.getExpectedAmountGotten().toString()
                                + " ETH"
                                + ". Your ETH balance is " + finalBalanceEth.toString() + " ETH."
                                + " Your BTC balance is " + finalBalanceBtc.toString() + " BTC.")
                        .title("BTC to ETH Transfer")
                        .build()
        );

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("Sent BTC TxHash", toSystemHash);
        txHashes.put("Received ETH TxHash", recipientHash);

        return txHashes;
    }

    @Transactional
    private Map<String, String> confirmEthToBtc(ExchangeDto exchangeDto) {

        if (exchangeDto.getAmountToExchange().compareTo(MIN_ETH_AMOUNT) < 0) {
            throw new InsufficientBalanceException
                    ("Exchange amount is too low. Minimum is " + MIN_ETH_AMOUNT + " ETH.");
        }

        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet")
                );

        if (ethereumWallet.isTradingLocked()) {
            throw new WalletLockedException("Wallet is currently in a transaction, try again later");
        }

        BigInteger updatedBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal updatedBalanceEth = Converter.convertWeiToEth(updatedBalanceWei);

        ethereumWallet.setBalance(updatedBalanceEth);
        ethereumWallet.setTradingLocked(true);
        ethereumWalletRepository.save(ethereumWallet);

        BigInteger gasPriceWei = exchangeDto.getFeesDto().getMeasure();

        BigDecimal totalFeesEth = exchangeDto
                .getFeesDto().getSystemFee().add(exchangeDto.getFeesDto().getTransactionFee());
        BigDecimal totalEth = exchangeDto.getAmountToExchange().add(totalFeesEth);
        BigInteger totalWei = Converter.convertEthToWei(totalEth);

        if (totalWei.compareTo(updatedBalanceWei) > 0) {
            throw new InsufficientBalanceException("Insufficient balance. You have "
                    + Converter.convertWeiToEth(updatedBalanceWei).toString()
            + " ETH");
        }

        Credentials credentials = Credentials.create(ethereumWallet.getPrivateKey());

        String toSystemHash = sendEtherToAddress(systemEthAddress,
                exchangeDto.getAmountToExchange(),
                gasPriceWei,
                credentials
        );

        BigInteger finalBalanceWei = getBalance(ethereumWallet.getAddress());
        BigDecimal finalBalanceEth = Converter.convertWeiToEth(finalBalanceWei);

        ethereumWallet.setBalance(finalBalanceEth);
        ethereumWallet.setTradingLocked(false);
        ethereumWalletRepository.save(ethereumWallet);

        BigInteger feeRate = externalApiHandler.getRecommendedBitcoinFee();

        BigDecimal amountGottenBtc = exchangeDto.getExpectedAmountGotten();
        BigInteger amountGottenSats = Converter.convertBtcToSats(amountGottenBtc);

        String recipientHash = bitcoinRPC.sendBitcoinFromSystem(
                bitcoinWallet.getAddress(),
                amountGottenSats,
                feeRate
        );

        BigDecimal finalBalanceBtc = bitcoinRPC.updateBalance(bitcoinWallet);
        bitcoinWallet.setBalance(finalBalanceBtc);
        bitcoinWalletRepository.save(bitcoinWallet);

        Exchange exchange = Exchange.builder()
                .exchangeType(ExchangeType.ETH_TO_BTC)
                .exchangeRate(exchangeDto.getRate())
                .amountGiven(exchangeDto.getAmountToExchange())
                .amountGotten(exchangeDto.getExpectedAmountGotten())
                .status(ExchangeStatus.COMPLETED)
                .user(user)
                .bitcoinWallet(bitcoinWallet)
                .ethereumWallet(ethereumWallet)
                .transactionFee(exchangeDto.getFeesDto().getTransactionFee())
                .systemFee(exchangeDto.getFeesDto().getSystemFee())
                .totalCost(totalEth)
                .createdAt(Date.from(Instant.now()))
                .build();

        exchangeRepository.save(exchange);

        refreshExchanges(
                new PaginationRequest(0,
                        10,
                        "id",
                        Sort.Direction.DESC)
        );

        notificationProducer.sendMail(
                NotificationEmail.builder()
                        .recipient(user.getEmail())
                        .subject("Successful ETH to BTC Transfer")
                        .body("You have used " + totalEth.toString()
                                + " ETH and gotten " + exchangeDto.getExpectedAmountGotten().toString()
                                + " BTC"
                                + ". Your ETH balance is " + finalBalanceEth.toString() + " ETH."
                        + " Your BTC balance is " + finalBalanceBtc.toString() + " BTC.")
                        .title("ETH to BTC Transfer")
                        .build()
        );

        Map<String, String> txHashes = new HashMap<>();
        txHashes.put("Sent ETH TxHash", toSystemHash);
        txHashes.put("Received BTC TxHash", recipientHash);

        return txHashes;
    }

    @SneakyThrows
    private String sendEtherToAddress(String recipientAddress,
                                      BigDecimal amountInEther,
                                      BigInteger gasPriceWei,
                                      Credentials credentials){

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
            throw new TransactionException("Transaction Error: " + response.getError().getMessage());
        }

        return response.getTransactionHash(); // TXID
    }

    @SneakyThrows
    private Credentials loadSystemWalletCredentials() {
        return WalletUtils.loadCredentials(keystorePassword, new File(keystorePath));
    }

    @SneakyThrows
    private BigInteger getBalance(String address) {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return balance.getBalance(); // in wei
    }

    private ExchangeDto mapExchangeToExchangeDto(Exchange exchange){
        return ExchangeDto.builder()
                .amountToExchange(exchange.getAmountGiven())
                .exchangeType(exchange.getExchangeType().name())
                .expectedAmountGotten(exchange.getAmountGotten())
                .rate(exchange.getExchangeRate())
                .build();
    }
}
