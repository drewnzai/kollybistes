package com.kollybistes.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDto {
    private String recipientAddress;
    private String senderAddress;
    private FeesDto feesDto;
    private BigDecimal amount;
    private BigDecimal expectedBalance;
    private String transactionHash;
    private Date createdAt;
}
