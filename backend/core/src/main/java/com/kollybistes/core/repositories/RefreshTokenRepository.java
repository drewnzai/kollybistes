package com.kollybistes.core.repositories;

import com.kollybistes.core.models.RefreshToken;
import com.kollybistes.core.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByToken(String refreshToken);
    RefreshToken findByTokenAndUser(String refreshToken, User user);

}