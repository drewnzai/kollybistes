package com.kollybistes.core.repositories;

import com.kollybistes.common.models.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Exchange, Long> {
}
