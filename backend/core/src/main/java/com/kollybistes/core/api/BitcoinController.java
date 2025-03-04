package com.kollybistes.core.api;

import com.kollybistes.common.dtos.APIResponse;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.core.services.BitcoinService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bitcoin/")
@AllArgsConstructor
public class BitcoinController {

    private final BitcoinService bitcoinService;

    @PostMapping("wallet/create")
    public Object createWallet() {
        try {
            return bitcoinService.createWallet();
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
            return bitcoinService.getWalletBalance();
        } catch (Exception e) {
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @PostMapping("send")
    public Object sendBitcoinToOutsideWallet(@RequestBody TransactionDto transactionDto){
        try{
            return bitcoinService.sendBitcoinToOutsideWallet(transactionDto.getRecipientAddress(),
                    transactionDto.getAmount());
        }catch(Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @PostMapping("confirmTransaction")
    public Object confirmTransaction(@RequestBody TransactionDto transactionDto){
        try{
            return bitcoinService.confirmTransaction(transactionDto);
        }catch(Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

}
