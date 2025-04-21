package com.kollybistes.common.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String senderWalletAddress;
    private String recipientWalletAddress;
    private BigDecimal amount;
    private String transactionHash;
}
