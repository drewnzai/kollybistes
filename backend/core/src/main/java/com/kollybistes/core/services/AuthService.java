package com.kollybistes.core.services;

import com.kollybistes.common.dtos.LoginRequest;
import com.kollybistes.common.dtos.LoginResponse;
import com.kollybistes.common.dtos.RefreshTokenRequest;
import com.kollybistes.common.dtos.RegisterRequest;
import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.common.models.RefreshToken;
import com.kollybistes.common.models.User;
import com.kollybistes.common.models.VerificationToken;
import com.kollybistes.core.auth.JwtUtil;
import com.kollybistes.core.auth.UserDetailsImpl;
import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.kafka.NotificationProducer;
import com.kollybistes.core.repositories.RefreshTokenRepository;
import com.kollybistes.core.repositories.UserRepository;
import com.kollybistes.core.repositories.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final NotificationProducer notificationProducer;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public void signup(RegisterRequest registerRequest)  {
        if(userRepository.existsByUsername(registerRequest.getUsername())){
            throw new ResourceAlreadyExistsException("Username already exists");
        }
        else if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new ResourceAlreadyExistsException("Email is already in use");
        }
        else{
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEnabled(false);

            String token = generateVerificationToken(user);

            notificationProducer.sendMail(
                    NotificationEmail.builder()
                            .subject("Account Verification")
                            .recipient(user.getEmail())
                            .title("Kollybistes Account Verification")
                            .body("Thank you for signing up to Kollybistes, " +
                                    "please click on the below url to activate your account: " +
                                    "http://localhost:8080/api/auth/accountVerification/" + token)
                            .build()
            );

            userRepository.save(user);
        }
    }

    private void fetchUserAndEnable(VerificationToken verificationToken)  {
        String username = verificationToken.getUser().getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new EntityNotFoundException("Could not find user")
        );
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public void verifyAccount(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(
                        () -> new EntityNotFoundException("Could not find verification token")
                );
        fetchUserAndEnable(verificationToken);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.
                getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(
                        () -> new EntityNotFoundException("Could not find user")
                );
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(
                () -> new UsernameNotFoundException("Could not find user")
        );

        if(!user.isEnabled()){
            throw new UserNotVerifiedException("User is not verified. Please check email and verify");
        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpirationDate(Instant.now().plusSeconds(2592000));
        refreshToken.setUser(user);
        refreshTokenRepository.save(refreshToken);

        return build(jwtUtil.generateJwtToken(authentication)
                , loginRequest.getUsername()
                , refreshToken.getToken());
    }

    public LoginResponse refresh(RefreshTokenRequest refreshTokenRequest) {

        User user = userRepository.findByUsername(refreshTokenRequest.getUsername()).orElseThrow(
                () -> new EntityNotFoundException("Could not find user")
        );
        RefreshToken refreshToken = refreshTokenRepository.
                findByTokenAndUser(refreshTokenRequest.getRefreshToken(), user)
                .orElseThrow(
                        () -> new EntityNotFoundException("Could not find refresh token")
                );

        boolean isNotExpired = Instant.now().isBefore(refreshToken.getExpirationDate());

        if(isNotExpired){
            return build(jwtUtil.generateJwtTokenFromUsername(refreshTokenRequest.getUsername())
                    , refreshTokenRequest.getUsername()
                    , refreshTokenRequest.getRefreshToken());
        }
        else if(!isNotExpired){
            refreshTokenRepository.deleteByToken(refreshTokenRequest.getRefreshToken());

            throw new ExpiredTokenException("Refresh Token has expired");
        }
        else {
            throw new IllegalFormatException("Refresh Token is not valid");
        }
    }

    private LoginResponse build(String token, String username, String refreshToken){
        return LoginResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtUtil.getJwtExpiration()))
                .username(username)
                .build();
    }

    public boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
    }

}
