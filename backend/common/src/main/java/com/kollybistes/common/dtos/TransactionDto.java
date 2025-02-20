package com.kollybistes.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDto {
    private String recipientAddress;
    private FeesDto feesDto;
    private BigDecimal amount;
    private BigDecimal expectedBalance;
}
