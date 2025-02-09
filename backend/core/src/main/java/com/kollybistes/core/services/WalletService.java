package com.kollybistes.core.services;

import com.kollybistes.core.dtos.WalletDto;
import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.UserRepository;
import com.kollybistes.core.rpc.BitcoinRPC;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final BitcoinRPC bitcoinRPC;

    @Value("${bitcoin.network}")
    private String bitcoinNetwork;

    public WalletDto createWallet(){
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinRPC.createWallet(user);

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public String getWalletBalance(Long id){
        User user = userRepository.findById(id).get();

        return bitcoinRPC.getAddressBalance(user.getUsername(), user.getUsername());
    }



}
