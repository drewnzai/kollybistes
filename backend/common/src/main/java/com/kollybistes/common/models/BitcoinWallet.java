package com.kollybistes.common.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "bitcoin_wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BitcoinWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String address;
    private BigDecimal balance;
    private String privateKey;
    private String publicKey;
    @CreationTimestamp
    @Column(name = "created_at")
    private Date createdAt;
    @OneToOne
    private User user;
}
