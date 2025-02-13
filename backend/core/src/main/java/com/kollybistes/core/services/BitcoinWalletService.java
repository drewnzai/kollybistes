package com.kollybistes.core.services;


import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BitcoinWalletService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;

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



}
