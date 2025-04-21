package com.kollybistes.core.services;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.EthereumRepository;
import com.kollybistes.core.repositories.ExchangeRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${system.btc.address}")
    private String systemBtcAddress;

    @Value("${system.eth.address}")
    private String systemEthAddress;

    @Value("${system.eth.chainId}")
    private String ethChainId;

    public ExchangeDto calculateExchangeDetails(ExchangeDto exchangeDto){
        if(exchangeDto.getTradeType().equalsIgnoreCase("BTC_TO_ETH")){
            return calculateBtcToEth(exchangeDto);
        }
        else{
            return calculateEthToBtc(exchangeDto);
        }
        return null;
    }

    private ExchangeDto calculateBtcToEth(ExchangeDto exchangeDto) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(() -> new Exception("User does not have a Bitcoin wallet"));


    }

    private BigInteger updateBalance(BitcoinWallet bitcoinWallet){
        BigInteger updated = bitcoinRPC
                .getTrustedAddressBalance(bitcoinWallet
                        .getUser()
                        .getUsername());

        bitcoinWallet.setBalance(updated);

        bitcoinWalletRepository.save(bitcoinWallet);

        return updated;
    }

}
