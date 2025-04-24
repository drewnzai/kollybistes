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

@Tag(name = "Wallet API", description = "The API for Crypto Wallet related functionality")
public interface GenericCryptoApi {

    @Operation(
            summary = "Crypto Wallet Creation",
            description = "Creates a Crypto Wallet for a user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "successful Crypto Wallet creation"),
            @ApiResponse(responseCode = "400", description = "could not create account")
    })
    ResponseEntity<WalletDto> createWallet();

    @Operation(
            summary = "Gets Wallet Balance",
            description = "Gets the balance currently held in a user's wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful balance acquisition"),
            @ApiResponse(responseCode = "400", description = "could not get the balance"),
            @ApiResponse(responseCode = "404", description = "wallet does not exist or is currently locked")
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
