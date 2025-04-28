package com.kollybistes.core.api.swaggerinterfaces;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.core.misc.PagingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Transaction API", description = "The API for Transaction related functionality")
public interface TransactionApi {

    @Operation(
            summary = "Get All BitcoinTransactions",
            description = "Gets all transactions by a given Bitcoin wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful transactions acquisition")
    })
    ResponseEntity<PagingResult<TransactionDto>> getBitcoinTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    );

    @Operation(
            summary = "Get All Ethereum Transactions",
            description = "Gets all transactions by a given Ethereum wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful transactions acquisition")
    })
    ResponseEntity<PagingResult<TransactionDto>> getEthereumTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    );
}
