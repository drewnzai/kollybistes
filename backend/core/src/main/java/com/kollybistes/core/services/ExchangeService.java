package com.kollybistes.core.services;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.common.dtos.FeesDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.ExchangeType;
import com.kollybistes.common.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.EthereumRepository;
import com.kollybistes.core.repositories.ExchangeRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    public ExchangeDto calculateExchangeDetails(ExchangeDto exchangeDto) throws Exception {
        if(exchangeDto.getExchangeType().equals("BTC_TO_ETH")){
            return calculateBtcToEth(exchangeDto);
        }
        else if(exchangeDto.getExchangeType().equals("ETH_TO_BTC")){
            return calculateEthToBtc(exchangeDto);
        }
        return null;
    }

    private ExchangeDto calculateBtcToEth(ExchangeDto exchangeDto) throws Exception {
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
            throw new Exception("Insufficient balance.");
        }

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
                .build();
    }

    private ExchangeDto calculateEthToBtc(ExchangeDto exchangeDto) throws Exception{
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
        BigInteger totalGasFees = gasPriceWei.multiply(gasLimit).multiply(BigInteger.TWO);

        BigDecimal exchangeRate = apiHandler.getBtcToEthExchangeRate();
        BigDecimal expectedReturnBtc = amountInEth.divide(exchangeRate);

        BigInteger totalCost = amountInWei.add(systemFeeWei).add(totalGasFees);

        if (totalCost.compareTo(balanceWei) > 0) {
            throw new Exception("User does not have the necessary balance");
        }

        return ExchangeDto.builder()
                .amountToExchange(amountInEth)
                .exchangeType(ExchangeType.ETH_TO_BTC.name())
                .expectedAmountGotten(expectedReturnBtc)
                .expectedBalance(convertWeiToEth(balanceWei.subtract(totalCost)))
                .feesDto(
                        new FeesDto(convertWeiToEth(systemFeeWei),
                                convertWeiToEth(totalGasFees), gasPriceWei)
                )
                .build();
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
