package com.kollybistes.core.api.swaggerinterfaces;

import com.kollybistes.common.dtos.LoginRequest;
import com.kollybistes.common.dtos.LoginResponse;
import com.kollybistes.common.dtos.RefreshTokenRequest;
import com.kollybistes.common.dtos.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "The Authentication API")
public interface AuthApi {

    @Operation(
            summary = "Sign up a new user",
            description = "Signs up a user after performing necessary checks"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "successful account creation")
    })
    ResponseEntity<String> signup(@RequestBody RegisterRequest registerRequest);

    @Operation(
            summary = "Verify a new account",
            description = "Verifies a user's verification token to enable their account"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful account verification")
    })
    ResponseEntity<String> verifyAccount(@PathVariable String token);

    @Operation(
            summary = "Login",
            description = "Authenticate a user's credentials"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful login")
    })
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest);

    @Operation(
            summary = "Refresh JWT",
            description = "Refreshes a user's JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful refresh")
    })
    ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest);
}
