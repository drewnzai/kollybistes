package com.kollybistes.core.mappers;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.common.models.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "feesDto", ignore = true)
    @Mapping(target = "expectedBalance", ignore = true)
    TransactionDto transactionToTransactionDto(Transaction transaction);
}
