package com.kollybistes.core.api;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.services.ExchangeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange/")
@AllArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping
    public PagingResult<ExchangeDto> getExchanges(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    ){
        final PaginationRequest paginationRequest = new PaginationRequest(page, size, sortField, direction);

        return exchangeService
                .getExchanges(paginationRequest);
    }

    @PostMapping("calculate")
    public Object calculateExchangeDetails(@RequestBody ExchangeDto exchangeDto) throws Exception {
            return exchangeService.calculateExchangeDetails(exchangeDto);
    }

    @PostMapping("confirm")
    public Object confirmExchange(@RequestBody ExchangeDto exchangeDto) throws Exception {
            return exchangeService.confirmExchange(exchangeDto);
    }
}
