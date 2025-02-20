package com.kollybistes.core.services;


import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BitcoinService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;
    private final ExchangeService exchangeService;

    @Value("${system.btc.address}")
    private String systemAddress;
    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.15");

    public WalletDto createWallet() throws Exception {
        User user = authService.getCurrentUser();

        if(bitcoinWalletRepository.existsByUser(user)){
            throw new Exception("User already has a wallet");
        }

        BitcoinWallet bitcoinWallet = bitcoinRPC.createWallet();

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public WalletDto getWalletBalance() throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                        .orElseThrow(
                                () -> {
                                    return new Exception("User does not have a Bitcoin wallet");
                                }
                        );
        bitcoinWallet.setBalance(
                bitcoinRPC.getTrustedAddressBalance(user.getUsername()));

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public String sendBitcoinToOutsideWallet(String recipientAddress, BigDecimal amount) throws Exception {
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> {
                            return new Exception("User does not have a Bitcoin wallet");
                        }
                );
        BigDecimal transactionAmount = amount.multiply(TRANSACTION_FEE_PERCENT); //Bitcoin to be sent to the system
        BigDecimal basicBTCFee =exchangeService.getRecommendedBitcoinFee();
        BigDecimal finalBTCFee = basicBTCFee.multiply(new BigDecimal("2.0")); //Gets the total amount of transaction fees that both transactions need
        BigDecimal finalAmount = amount.add(transactionAmount).add(finalBTCFee);

        if(finalAmount.compareTo(updateBalance(bitcoinWallet)) >= 0){

            bitcoinRPC.sendBitcoinToSystem(
                    user.getUsername(),
                    systemAddress,
                    transactionAmount,
                    basicBTCFee
            );

            return bitcoinRPC.sendBitcoin(
                    user.getUsername(), 
                    recipientAddress,
                    amount,
                    basicBTCFee
            );

        }
        else{
            throw new Exception("User does not have the necessary balance");
        }
    }

    private BigDecimal updateBalance(BitcoinWallet bitcoinWallet){
        BigDecimal updated = bitcoinRPC
                .getTrustedAddressBalance(bitcoinWallet
                        .getUser()
                        .getUsername());

        bitcoinWallet.setBalance(updated);

        bitcoinWalletRepository.save(bitcoinWallet);

        return updated;
    }

}
