package com.kollybistes.core.api.swaggerinterfaces;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.dtos.WalletDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Tag(name = "Bitcoin", description = "The API for Bitcoin Wallet related functionality")
public interface BitcoinApi {

    @Operation(
            summary = "Bitcoin Wallet Creation",
            description = "Creates a Bitcoin Wallet for a user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "successful Bitcoin Wallet creation"),
            @ApiResponse(responseCode = "400", description = "could not create account")
    })
    ResponseEntity<WalletDto> createWallet() throws Exception;

    @Operation(
            summary = "Sign up a new user",
            description = "Signs up a user after performing necessary checks"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "successful account creation"),
            @ApiResponse(responseCode = "400", description = "could not create account")
    })
    ResponseEntity<WalletDto> getBalance();

    @Operation(
            summary = "Calculates transaction requirements",
            description = "Calculates a transaction's requirements and provides the details to the user " +
                    "before confirmation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful requirements acquisition"),
            @ApiResponse(responseCode = "400", description = "could not get requirements"),
            @ApiResponse(responseCode = "404", description = "wallet not found")
    })
    ResponseEntity<TransactionDto> calculateTransactionDetails(@RequestBody TransactionDto transactionDto);

    @Operation(
            summary = "Confirm transaction requirements",
            description = "Confirms a transaction and performs the transaction, " +
                    "sending crypto to user and outside wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful transaction"),
            @ApiResponse(responseCode = "400", description = "could not fulfill transaction"),
            @ApiResponse(responseCode = "404", description = "wallet not found")
    })
    ResponseEntity<Map<String, String>> confirmTransaction(@RequestBody TransactionDto transactionDto);
}
