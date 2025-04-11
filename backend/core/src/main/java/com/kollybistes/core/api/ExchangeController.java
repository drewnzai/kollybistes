package com.kollybistes.core.api;

import com.kollybistes.common.dtos.APIResponse;
import com.kollybistes.core.services.ExchangeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange/")
@AllArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("all")
    public Object getAll(){
        try{
            return exchangeService.getAllFees();
        }
        catch (Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }
}
