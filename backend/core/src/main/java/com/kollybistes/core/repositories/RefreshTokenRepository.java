package com.kollybistes.core.repositories;

import com.kollybistes.common.models.RefreshToken;
import com.kollybistes.common.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByToken(String refreshToken);
    Optional<RefreshToken> findByTokenAndUser(String refreshToken, User user);

}