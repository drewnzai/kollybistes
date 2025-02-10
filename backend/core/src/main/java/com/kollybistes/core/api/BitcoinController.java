package com.kollybistes.core.api;

import com.kollybistes.core.dtos.WalletDto;
import com.kollybistes.core.services.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/bitcoin/")
public class BitcoinController {

    @Autowired
    private WalletService walletService;

    @PostMapping("wallet/create")
    public WalletDto createWallet() {
        return walletService.createWallet();
    }

    @GetMapping("wallet/balance")
    public String getBalance(@PathVariable("userId") Long userId) {
        return walletService.getWalletBalance(userId);
    }

}
