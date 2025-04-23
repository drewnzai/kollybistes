package com.kollybistes.core.services;

import com.kollybistes.common.models.Transaction;
import com.kollybistes.core.repositories.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

@Service
@AllArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Async
    public void saveTransaction(String sender,
                                String recipient,
                                BigDecimal amount,
                                String txHash){
        
        Transaction transaction = Transaction.builder()
                .senderWalletAddress(sender)
                .recipientWalletAddress(recipient)
                .amount(amount)
                .transactionHash(txHash)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactionRepository.save(transaction);
    }
}
