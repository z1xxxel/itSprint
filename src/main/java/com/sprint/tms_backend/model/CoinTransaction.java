package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_transactions", indexes = {
        @Index(name = "idx_coin_tx_from", columnList = "from_user_id"),
        @Index(name = "idx_coin_tx_to", columnList = "to_user_id"),
        @Index(name = "idx_coin_tx_created", columnList = "created_at")
})
@Data
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null = эмиссия (EVENT_REWARD, MANAGER_BONUS, ACHIEVEMENT, ADMIN_ADJUSTMENT)
    @Column(name = "from_user_id")
    private Long fromUserId;

    // null = сжигание (PURCHASE)
    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoinTransactionType type;

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ThanksTag tag;

    @Column(nullable = false)
    private Boolean isCancelled = false; // Отменена ли транзакция

    @Column(length = 500)
    private String cancellationReason; // Причина отмены админом
}