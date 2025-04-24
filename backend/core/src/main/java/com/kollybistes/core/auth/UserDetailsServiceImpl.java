package com.kollybistes.core.auth;

import com.kollybistes.common.models.User;
import com.kollybistes.core.exceptions.UserNotVerifiedException;
import com.kollybistes.core.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("Username not found")
        );

        if(!user.isEnabled()){
            throw new UserNotVerifiedException("User is not verified. Please check email and verify");
        }

        return new UserDetailsImpl(user);
    }

}
