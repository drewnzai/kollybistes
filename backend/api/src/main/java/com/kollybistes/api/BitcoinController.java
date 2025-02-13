package com.kollybistes.api;

import com.kollybistes.common.dtos.APIResponse;
import com.kollybistes.core.services.BitcoinWalletService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bitcoin/")
@AllArgsConstructor
public class BitcoinController {

    private final BitcoinWalletService bitcoinWalletService;

    @PostMapping("wallet/create")
    public Object createWallet() {
        try {
            return bitcoinWalletService.createWallet();
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
            return bitcoinWalletService.getWalletBalance();
        } catch (Exception e) {
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

}
