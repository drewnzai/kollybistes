package com.kollybistes.core.services;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.models.Transaction;
import com.kollybistes.common.models.User;
import com.kollybistes.core.mappers.TransactionMapper;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.repositories.TransactionRepository;
import com.kollybistes.core.util.PaginationUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AuthService authService;

    @Async
    public void saveTransaction(String sender,
                                String recipient,
                                BigDecimal amount,
                                String txHash){
        
        Transaction transaction = Transaction.builder()
                .senderAddress(sender)
                .recipientAddress(recipient)
                .amount(amount)
                .transactionHash(txHash)
                .createdAt(Date.from(Instant.now()))
                .build();

        transactionRepository.save(transaction);
    }

    public PagingResult<TransactionDto> getTransactions(PaginationRequest paginationRequest){
        User user = authService.getCurrentUser();
        
        final Pageable pageable = PaginationUtil.getPageable(paginationRequest);
        Page<Transaction> userTransactions = transactionRepository.findAllByUser(user, pageable);
        List<TransactionDto> transactions = userTransactions
                .stream()
                .map(transactionMapper::transactionToTransactionDto).toList();

        return new PagingResult<>(
                transactions,
                userTransactions.getTotalPages(),
                userTransactions.getTotalElements(),
                userTransactions.getSize(),
                userTransactions.getNumber(),
                userTransactions.isEmpty()
        );
    }
}
