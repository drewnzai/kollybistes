package com.kollybistes.core.services;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.EthereumWallet;
import com.kollybistes.common.models.Transaction;
import com.kollybistes.common.models.User;
import com.kollybistes.core.exceptions.EntityNotFoundException;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.EthereumWalletRepository;
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
    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final EthereumWalletRepository ethereumWalletRepository;
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

    public PagingResult<TransactionDto> getBitcoinTransactions(PaginationRequest paginationRequest){
        User user = authService.getCurrentUser();

        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have a Bitcoin wallet")
                );
        
        final Pageable pageable = PaginationUtil.getPageable(paginationRequest);
        Page<Transaction> bitcoinTransactions = transactionRepository.findAllBySenderAddress(bitcoinWallet.getAddress(),
                pageable);
        List<TransactionDto> transactions = bitcoinTransactions
                .stream()
                .map(this::mapTransactionToTransactionDto).toList();

        return new PagingResult<>(
                transactions,
                bitcoinTransactions.getTotalPages(),
                bitcoinTransactions.getTotalElements(),
                bitcoinTransactions.getSize(),
                bitcoinTransactions.getNumber(),
                bitcoinTransactions.isEmpty()
        );
    }

    public PagingResult<TransactionDto> getEthereumTransactions(PaginationRequest paginationRequest){
        User user = authService.getCurrentUser();

        EthereumWallet ethereumWallet = ethereumWalletRepository.findByUser(user)
                .orElseThrow(
                        () -> new EntityNotFoundException("User does not have an Ethereum wallet")
                );

        final Pageable pageable = PaginationUtil.getPageable(paginationRequest);
        Page<Transaction> ethereumTransactions = transactionRepository.findAllBySenderAddress(ethereumWallet.getAddress(),
                pageable);
        List<TransactionDto> transactions = ethereumTransactions
                .stream()
                .map(this::mapTransactionToTransactionDto).toList();

        return new PagingResult<>(
                transactions,
                ethereumTransactions.getTotalPages(),
                ethereumTransactions.getTotalElements(),
                ethereumTransactions.getSize(),
                ethereumTransactions.getNumber(),
                ethereumTransactions.isEmpty()
        );
    }

    private TransactionDto mapTransactionToTransactionDto(Transaction transaction){
        return TransactionDto.builder()
                .senderAddress(transaction.getSenderAddress())
                .amount(transaction.getAmount())
                .createdAt(transaction.getCreatedAt())
                .transactionHash(transaction.getTransactionHash())
                .recipientAddress(transaction.getRecipientAddress())
                .build();
    }
}
