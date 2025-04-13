package com.kollybistes.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeesDto {
    private BigDecimal systemFee;
    private BigDecimal transactionFee;
    private BigInteger measure;
}
