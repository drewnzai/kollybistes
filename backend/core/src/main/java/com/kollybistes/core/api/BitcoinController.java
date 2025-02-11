package com.kollybistes.core.api;

import com.kollybistes.core.dtos.APIResponse;
import com.kollybistes.core.services.WalletService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bitcoin/")
@AllArgsConstructor
public class BitcoinController {

    private final WalletService walletService;

    @PostMapping("wallet/create")
    public Object createWallet() {
        try {
            return walletService.createWallet();
        } catch (Exception e) {
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @GetMapping("wallet/balance")
    public Object getBalance() {
        try {
            return walletService.getWalletBalance();
        } catch (Exception e) {
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

}
