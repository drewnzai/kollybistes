package com.kollybistes.core.repositories;

import com.kollybistes.common.models.Exchange;
import com.kollybistes.common.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    Page<Exchange> findAllByUser(User user, Pageable pageable);
}
