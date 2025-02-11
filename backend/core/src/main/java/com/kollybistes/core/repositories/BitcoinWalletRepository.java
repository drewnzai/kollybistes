package com.kollybistes.core.repositories;

import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BitcoinWalletRepository extends JpaRepository<BitcoinWallet, Long> {
    Optional<BitcoinWallet> findByUser(User user);

    boolean existsByUser(User user);
}
