package com.kollybistes.core.api;

import com.kollybistes.core.dtos.WalletDto;
import com.kollybistes.core.services.WalletService;
import org.bitcoinj.store.BlockStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bitcoin/wallets")
public class BitcoinController {

    @Autowired
    private WalletService walletService;

    @PostMapping("")
    public WalletDto createWallet() {
        return walletService.createWallet();
    }

    @GetMapping("/balance/{userId}")
    public WalletDto getBalance(@PathVariable("userId") Long userId) throws BlockStoreException {
        return walletService.getBalance(userId);
    }

}
