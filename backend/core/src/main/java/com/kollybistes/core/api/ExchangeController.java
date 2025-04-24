package com.kollybistes.core.api;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.core.api.swaggerinterfaces.ExchangeApi;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.services.ExchangeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exchange/")
@AllArgsConstructor
public class ExchangeController implements ExchangeApi {

    private final ExchangeService exchangeService;

    @GetMapping
    @Override
    public ResponseEntity<PagingResult<ExchangeDto>> getExchanges(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    ){
        final PaginationRequest paginationRequest = new PaginationRequest(page, size, sortField, direction);

        return new ResponseEntity<>(exchangeService
                .getExchanges(paginationRequest),
                HttpStatus.OK);
    }

    @PostMapping("calculate")
    @Override
    public ResponseEntity<ExchangeDto> calculateExchangeDetails(@RequestBody ExchangeDto exchangeDto) {
            return new ResponseEntity<>(exchangeService.
                    calculateExchangeDetails(exchangeDto),
                    HttpStatus.OK);
    }

    @PostMapping("confirm")
    @Override
    public ResponseEntity<Map<String, String>> confirmExchange(@RequestBody ExchangeDto exchangeDto) {
            return new ResponseEntity<>(exchangeService.
                    confirmExchange(exchangeDto),
                    HttpStatus.OK);
    }
}
