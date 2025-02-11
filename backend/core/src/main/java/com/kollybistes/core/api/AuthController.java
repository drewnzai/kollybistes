package com.kollybistes.core.api;

import com.kollybistes.core.dtos.APIResponse;
import com.kollybistes.core.dtos.LoginRequest;
import com.kollybistes.core.dtos.RefreshTokenRequest;
import com.kollybistes.core.dtos.RegisterRequest;
import com.kollybistes.core.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/")
@AllArgsConstructor
@Tag(name = "Authentication endpoint", description = "Provides a point for all authentication related functionality")
public class AuthController {

    private final AuthService authService;

    @PostMapping("signup")
    public Object signup(@RequestBody RegisterRequest registerRequest) throws Exception {

        try{
            authService.signup(registerRequest);
            return null;
        }
        catch(Exception e){
            return APIResponse.builder().error(e.getMessage()).build();
        }

    }

    @GetMapping("accountVerification/{token}")
    public String verifyAccount(@PathVariable String token) {

        try{
            authService.verifyAccount(token);
            return "Account Activated Successfully";
        }
        catch(Exception e){
            return "Account not activated";
        }

    }

    @PostMapping("login")
    public Object login(@RequestBody LoginRequest loginRequest){

        try{
            return authService.login(loginRequest);
        }
        catch(BadCredentialsException e){
            return APIResponse.builder().error("Wrong password").build();
        }
        catch(UsernameNotFoundException | NullPointerException e){
            return APIResponse.builder().error("Username does not exist").build();
        }
        catch(Exception e){
            return APIResponse.builder().error("Verify account").build();
        }

    }

    @PostMapping("refresh")
    public Object refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception{

        try{
            return authService.refresh(refreshTokenRequest);
        }
        catch(Exception e){
            return APIResponse.builder().error(e.getMessage()).build();
        }

    }

}