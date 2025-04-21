package com.kollybistes.core.api;

import com.kollybistes.common.dtos.APIResponse;
import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.core.services.EthereumService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ethereum/")
@AllArgsConstructor
public class EthereumController {

    private final EthereumService ethereumService;

    @GetMapping("wallet/create")
    public Object createWallet() {
        try {
            return ethereumService.createWallet();
        } catch (Exception e) {
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @PostMapping("send")
    public Object sendEthToOutsideWallet(@RequestBody TransactionDto transactionDto){
        try{
            return ethereumService.calculateTransactionDetails(transactionDto.getRecipientAddress()
                    , transactionDto.getAmount());
        }
        catch(Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @PostMapping("confirm")
    public Object confirmTransaction(@RequestBody TransactionDto transactionDto){
        try{
            return ethereumService.confirmTransactionToOutsideWallet(transactionDto);
        }catch(Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }
}
