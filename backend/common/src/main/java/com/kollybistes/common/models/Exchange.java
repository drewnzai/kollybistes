package com.kollybistes.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType; // BUY or SELL (BTC <-> ETH)

    @ManyToOne
    private BitcoinWallet bitcoinWallet; // BTC wallet used in the trade

    @ManyToOne
    private EthereumWallet ethereumWallet; // ETH wallet used in the trade

    private BigDecimal amountGiven; // Amount of crypto traded

    private BigDecimal amountGotten; // Amount of crypto received

    private BigDecimal exchangeRate; // BTC/ETH exchange rate at the time of trade

    private BigDecimal systemFee; // 15% of the trade amount

    private BigDecimal transactionFee; // Gas fee for faster transaction completion

    private BigDecimal totalCost; // Total cost including fees

    @Enumerated(EnumType.STRING)
    private ExchangeStatus status; // PENDING, COMPLETED, FAILED

    @CreationTimestamp
    @Column(name = "created_at")
    private Date createdAt;
}
