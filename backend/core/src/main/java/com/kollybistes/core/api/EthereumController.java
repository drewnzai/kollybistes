package com.kollybistes.core.api;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.core.services.EthereumService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ethereum/")
@AllArgsConstructor
public class EthereumController {

    private final EthereumService ethereumService;

    @GetMapping("wallet/create")
    public ResponseEntity<WalletDto> createWallet() throws Exception {
            return new ResponseEntity<>(ethereumService.createWallet(),
                    HttpStatus.CREATED);
    }

    @GetMapping("wallet/balance")
    public ResponseEntity<WalletDto> getBalance() throws Exception {
        return new ResponseEntity<>(ethereumService.getWalletBalance(),
                HttpStatus.OK);
    }

    @PostMapping("calculate")
    public ResponseEntity<TransactionDto> calculateTransactionDetails(@RequestBody TransactionDto transactionDto) throws Exception {
            return new ResponseEntity<>(ethereumService.
                    calculateTransactionDetails(transactionDto.getRecipientAddress(),
                            transactionDto.getAmount()),
                    HttpStatus.OK);
    }

    @PostMapping("confirm")
    public ResponseEntity<Map<String, String>> confirmTransaction(@RequestBody TransactionDto transactionDto) throws Exception {
            return new ResponseEntity<>(ethereumService.
                    confirmTransactionToOutsideWallet
                            (transactionDto),
                    HttpStatus.OK);
    }
}
