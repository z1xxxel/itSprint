package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record ShopAuditLogDTO(
        Long id,
        String action,
        Long actorId,
        String actorName,
        Long productId,
        String productName,
        Long orderId,
        String details,
        LocalDateTime createdAt
) {}