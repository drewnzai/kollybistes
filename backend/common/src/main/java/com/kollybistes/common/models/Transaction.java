package com.kollybistes.common.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String senderWalletAddress;
    private String recipientWalletAddress;
    private BigDecimal amount;
    private String transactionHash;
    @CreationTimestamp
    @Column(name = "created_at")
    private Date createdAt;
}
