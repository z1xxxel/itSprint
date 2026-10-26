package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_audit_logs", indexes = {
        @Index(name = "idx_audit_created", columnList = "createdAt")
})
@Data
public class ShopAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED, DISCOUNT_CREATED,
    // ORDER_APPROVED, ORDER_REJECTED, ORDER_COMPLETED, STOCK_WRITTEN_OFF
    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String actorName;

    private Long productId;
    private String productName;

    private Long orderId;

    @Column(length = 1000)
    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;
}