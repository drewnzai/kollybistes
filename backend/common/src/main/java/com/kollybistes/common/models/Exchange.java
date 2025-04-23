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
@Table(name = "exchanges")
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

    @Column(precision = 36, scale = 18)
    private BigDecimal amountGiven; // Amount of crypto traded

    @Column(precision = 36, scale = 18)
    private BigDecimal amountGotten; // Amount of crypto received

    @Column(precision = 19, scale = 9)
    private BigDecimal exchangeRate; // BTC/ETH exchange rate at the time of trade

    @Column(precision = 36, scale = 18)
    private BigDecimal systemFee; // 15% of the trade amount

    @Column(precision = 36, scale = 18)
    private BigDecimal transactionFee; // Gas fee or BTC transaction fee for faster transaction completion

    @Column(precision = 36, scale = 18)
    private BigDecimal totalCost; // Total cost including fees

    @Enumerated(EnumType.STRING)
    private ExchangeStatus status; // PENDING, COMPLETED, FAILED

    @CreationTimestamp
    @Column(name = "created_at")
    private Date createdAt;
}
