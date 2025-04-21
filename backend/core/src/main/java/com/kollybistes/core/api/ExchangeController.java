package com.kollybistes.core.api;

import com.kollybistes.common.dtos.APIResponse;
import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.core.services.ExchangeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange/")
@AllArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping("calculate")
    public Object calculateExchangeDetails(@RequestBody ExchangeDto exchangeDto){
        try{
            return exchangeService.calculateExchangeDetails(exchangeDto);
        }
        catch(Exception e){
            return APIResponse
                    .builder()
                    .error(e.getMessage())
                    .build();
        }
    }
}
