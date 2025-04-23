package com.kollybistes.core.api;


import com.kollybistes.core.util.ErrorResponse;
import com.kollybistes.common.dtos.LoginRequest;
import com.kollybistes.common.dtos.RefreshTokenRequest;
import com.kollybistes.common.dtos.RegisterRequest;
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
    public void signup(@RequestBody RegisterRequest registerRequest) {
            authService.signup(registerRequest);
    }

    @GetMapping("accountVerification/{token}")
    public String verifyAccount(@PathVariable String token) {
            authService.verifyAccount(token);
            return "Account Activated Successfully";
    }

    @PostMapping("login")
    public Object login(@RequestBody LoginRequest loginRequest){

        try{
            return authService.login(loginRequest);
        }
        catch(BadCredentialsException e){
            return ErrorResponse.builder().error("Wrong password").build();
        }
        catch(UsernameNotFoundException | NullPointerException e){
            return ErrorResponse.builder().error("Username does not exist").build();
        }
        catch(Exception e){
            return ErrorResponse.builder().error("Verify account").build();
        }

    }

    @PostMapping("refresh")
    public Object refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception{
            return authService.refresh(refreshTokenRequest);
    }

}