package com.kollybistes.core.api.swaggerinterfaces;

import com.kollybistes.common.dtos.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "The Authentication API")
public interface AuthApi {

    @Operation(
            summary = "Sign up a new user",
            description = "Signs up a user after performing necessary checks"
    )
    void signup(@RequestBody RegisterRequest registerRequest);
}
