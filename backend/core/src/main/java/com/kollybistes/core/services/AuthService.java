package com.kollybistes.core.services;

import com.kollybistes.common.dtos.LoginRequest;
import com.kollybistes.common.dtos.LoginResponse;
import com.kollybistes.common.dtos.RefreshTokenRequest;
import com.kollybistes.common.dtos.RegisterRequest;
import com.kollybistes.common.models.NotificationEmail;
import com.kollybistes.common.models.RefreshToken;
import com.kollybistes.common.models.User;
import com.kollybistes.common.models.VerificationToken;
import com.kollybistes.core.auth.JwtUtil;
import com.kollybistes.core.auth.UserDetailsImpl;
import com.kollybistes.core.repositories.RefreshTokenRepository;
import com.kollybistes.core.repositories.UserRepository;
import com.kollybistes.core.repositories.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public void signup(RegisterRequest registerRequest) throws Exception {
        if(userRepository.existsByUsername(registerRequest.getUsername())){
            throw new Exception("Username already exists");
        }
        else if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new Exception("Email is already in use");
        }
        else{
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEnabled(false);

            String token = generateVerificationToken(user);

            mailService.sendMail(
                    new NotificationEmail("Account Verification",
                            user.getEmail(),
                            "Thank you for signing up to Kollybistes, " +
                                    "please click on the below url to activate your account: " +
                                    "http://localhost:8080/api/auth/accountVerification/" + token)
            );

            userRepository.save(user);
        }
    }

    private void fetchUserAndEnable(VerificationToken verificationToken) throws Exception {
        String username = verificationToken.getUser().getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> {
                    return new Exception("Could not find user");
                }
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

    public void verifyAccount(String token) throws Exception {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
        fetchUserAndEnable(verificationToken);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.
                getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(principal.getUsername()).get();
    }

    public LoginResponse login(LoginRequest loginRequest) throws Exception {
        Authentication authenticate = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                        loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(
                () -> {
                    return new Exception("Could not find user");
                }
        );

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpirationDate(Instant.now().plusSeconds(2592000));
        refreshToken.setUser(user);
        refreshTokenRepository.save(refreshToken);

        return build(jwtUtil.generateJwtToken(authenticate)
                , loginRequest.getUsername()
                , refreshToken.getToken());
    }

    public LoginResponse refresh(RefreshTokenRequest refreshTokenRequest) throws Exception {

        User user = userRepository.findByUsername(refreshTokenRequest.getUsername()).orElseThrow(
                () -> {
                    return new Exception("Could not find user");
                }
        );
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndUser(refreshTokenRequest.getRefreshToken(), user);

        boolean isNotExpired = Instant.now().isBefore(refreshToken.getExpirationDate());

        if(isNotExpired){
            return build(jwtUtil.generateJwtTokenFromUsername(refreshTokenRequest.getUsername())
                    , refreshTokenRequest.getUsername()
                    , refreshTokenRequest.getRefreshToken());
        }
        else if(!isNotExpired){
            refreshTokenRepository.deleteByToken(refreshTokenRequest.getRefreshToken());

            throw new Exception("Refresh Token has expired");
        }
        else {
            throw new Exception("Refresh Token is not valid");
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
