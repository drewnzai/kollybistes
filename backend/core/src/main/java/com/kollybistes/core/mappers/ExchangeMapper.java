package com.kollybistes.core.mappers;

import com.kollybistes.common.dtos.ExchangeDto;
import com.kollybistes.common.models.Exchange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExchangeMapper {
    @Mapping(target = "exchangeType", source = "java(exchangeType.name())")
    @Mapping(target = "amountToExchange", source = "amountGiven")
    @Mapping(target = "expectedAmountGotten", source = "amountGotten")
    @Mapping(target = "expectedBalance", ignore = true)
    @Mapping(target = "rate", source = "exchangeRate")
    ExchangeDto exchangeToExchangeDto(Exchange exchange);
}
