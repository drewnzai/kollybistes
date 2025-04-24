package com.kollybistes.core.api;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.core.services.EthereumService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ethereum/")
@AllArgsConstructor
public class EthereumController {

    private final EthereumService ethereumService;

    @GetMapping("wallet/create")
    public WalletDto createWallet() throws Exception {
            return ethereumService.createWallet();
    }

    @PostMapping("calculate")
    public TransactionDto calculateTransactionDetails(@RequestBody TransactionDto transactionDto) throws Exception {
            return ethereumService.calculateTransactionDetails(transactionDto.getRecipientAddress()
                    , transactionDto.getAmount());
    }

    @PostMapping("confirm")
    public Map<String, String> confirmTransaction(@RequestBody TransactionDto transactionDto) throws Exception {
            return ethereumService.confirmTransactionToOutsideWallet(transactionDto);
    }
}
