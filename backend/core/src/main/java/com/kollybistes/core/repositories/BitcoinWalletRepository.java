package com.kollybistes.core.repositories;

import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitcoinWalletRepository extends JpaRepository<BitcoinWallet, Long> {
    BitcoinWallet findByUser(User user);
}
