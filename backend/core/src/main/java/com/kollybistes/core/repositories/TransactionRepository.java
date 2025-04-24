package com.kollybistes.core.repositories;

import com.kollybistes.common.models.Transaction;
import com.kollybistes.common.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findAllByUser(User user, Pageable pageable);
}
