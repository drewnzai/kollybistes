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
public class ExchangeDto {
    private String exchangeType;
    private BigDecimal amountToExchange;
    private FeesDto feesDto;
    private BigDecimal expectedAmountGotten;
    private BigDecimal expectedBalance;
    private BigDecimal rate;
}
