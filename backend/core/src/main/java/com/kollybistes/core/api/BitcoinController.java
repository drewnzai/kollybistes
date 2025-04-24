package com.kollybistes.core.api;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import com.kollybistes.core.api.swaggerinterfaces.GenericCryptoApi;
import com.kollybistes.core.services.BitcoinService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bitcoin/")
@AllArgsConstructor
public class BitcoinController implements GenericCryptoApi {

    private final BitcoinService bitcoinService;

    @PostMapping("wallet/create")
    @Override
    public ResponseEntity<WalletDto> createWallet() {
            return new ResponseEntity<>(bitcoinService.createWallet(), HttpStatus.CREATED);
    }

    @GetMapping("wallet/balance")
    @Override
    public ResponseEntity<WalletDto> getBalance() {
        return new ResponseEntity<>(bitcoinService.getWalletBalance(), HttpStatus.OK);
    }

    @PostMapping("calculate")
    @Override
    public ResponseEntity<TransactionDto> calculateTransactionDetails(@RequestBody TransactionDto transactionDto) {
            return new ResponseEntity<>(bitcoinService.
                    calculateTransactionDetails
                            (transactionDto.getRecipientAddress(),
                                    transactionDto.getAmount()),
                    HttpStatus.OK);
    }

    @PostMapping("confirm")
    @Override
    public ResponseEntity<Map<String, String>> confirmTransaction(@RequestBody TransactionDto transactionDto) {
            return new ResponseEntity<>(bitcoinService.
                    confirmTransactionToOutsideWallet
                            (transactionDto),
                    HttpStatus.OK);
    }

}
