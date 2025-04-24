package com.kollybistes.core.api;


import com.kollybistes.common.dtos.LoginRequest;
import com.kollybistes.common.dtos.LoginResponse;
import com.kollybistes.common.dtos.RefreshTokenRequest;
import com.kollybistes.common.dtos.RegisterRequest;
import com.kollybistes.core.api.swaggerinterfaces.AuthApi;
import com.kollybistes.core.services.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/")
@AllArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @PostMapping("signup")
    @Override
    public ResponseEntity<String> signup(@RequestBody RegisterRequest registerRequest) {
            authService.signup(registerRequest);
            return new ResponseEntity<>("Account created successfully," +
                    " check email for verification details",
                    HttpStatus.CREATED);
    }

    @GetMapping("accountVerification/{token}")
    @Override
    public ResponseEntity<String> verifyAccount(@PathVariable String token) {
            authService.verifyAccount(token);
            return new ResponseEntity<>("Account Activated Successfully",
                    HttpStatus.OK);
    }

    @PostMapping("login")
    @Override
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
            return new ResponseEntity<>(authService.login(loginRequest),
                    HttpStatus.OK);
    }

    @PostMapping("refresh")
    @Override
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
            return new ResponseEntity<>(authService.refresh(refreshTokenRequest),
                    HttpStatus.OK);
    }

}