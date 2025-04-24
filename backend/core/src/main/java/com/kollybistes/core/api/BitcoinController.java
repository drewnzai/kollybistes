package com.kollybistes.core.api;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.core.services.BitcoinService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bitcoin/")
@AllArgsConstructor
public class BitcoinController {

    private final BitcoinService bitcoinService;

    @PostMapping("wallet/create")
    public WalletDto createWallet() throws Exception {
            return bitcoinService.createWallet();
    }

    @GetMapping("wallet/balance")
    public WalletDto getBalance() throws Exception {
            return bitcoinService.getWalletBalance();
    }

    @PostMapping("calculate")
    public TransactionDto calculateTransactionDetails(@RequestBody TransactionDto transactionDto) throws Exception {
            return bitcoinService.calculateTransactionDetails(transactionDto.getRecipientAddress(),
                    transactionDto.getAmount());
    }

    @PostMapping("confirm")
    public Map<String, String> confirmTransaction(@RequestBody TransactionDto transactionDto) throws Exception {
            return bitcoinService.confirmTransactionToOutsideWallet(transactionDto);
    }

}
