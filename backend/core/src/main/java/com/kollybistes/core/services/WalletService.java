package com.kollybistes.core.services;

import com.kollybistes.core.dtos.WalletDto;
import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;

    public WalletDto createWallet(){
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinRPC.createWallet(user);

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public WalletDto getWalletBalance(){
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user);
        bitcoinWallet.setBalance(
                bitcoinRPC.getTrustedAddressBalance(user.getUsername()));

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }



}
