package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "treasury_transactions", indexes = {
        @Index(name = "idx_treasury_tx_created", columnList = "created_at")
})
@Data
public class TreasuryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TreasuryTxType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    private Long actorId;
    private String actorName;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}