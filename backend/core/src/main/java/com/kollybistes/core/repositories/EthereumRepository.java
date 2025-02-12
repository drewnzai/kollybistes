package com.kollybistes.core.repositories;

import com.kollybistes.core.models.EthereumWallet;
import com.kollybistes.core.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EthereumRepository extends JpaRepository<EthereumWallet, Long> {
    Optional<EthereumWallet> findByUser(User user);

    boolean existsByUser(User user);
}
