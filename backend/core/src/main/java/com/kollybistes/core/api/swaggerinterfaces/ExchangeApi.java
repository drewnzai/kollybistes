package com.kollybistes.core.api.swaggerinterfaces;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.core.misc.PagingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "Exchange API", description = "The API for Crypto exchanges related functionality")
public interface ExchangeApi {

    @Operation(
            summary = "Get All Exchanges",
            description = "Gets all exchanges by a given user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful exchanges acquisition"),
            @ApiResponse(responseCode = "404", description = "could not get exchanges")
    })
    ResponseEntity<PagingResult<ExchangeDto>> getExchanges(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    );

    @Operation(
            summary = "Calculates exchange requirements",
            description = "Calculates an exchange's requirements and provides the details to the user " +
                    "before confirmation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful requirements acquisition"),
            @ApiResponse(responseCode = "400", description = "could not get requirements"),
            @ApiResponse(responseCode = "404", description = "wallet not found")
    })
    ResponseEntity<ExchangeDto> calculateExchangeDetails(@RequestBody ExchangeDto exchangeDto);

    @Operation(
            summary = "Confirm exchange requirements",
            description = "Confirms an exchange and performs the exchange, " +
                    "sending crypto to user and outside wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful exchange"),
            @ApiResponse(responseCode = "400", description = "could not fulfill exchange"),
            @ApiResponse(responseCode = "404", description = "wallet not found")
    })
    ResponseEntity<Map<String, String>> confirmExchange(@RequestBody ExchangeDto exchangeDto);
}
