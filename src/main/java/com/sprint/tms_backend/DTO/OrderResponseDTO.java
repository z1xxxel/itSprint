package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponseDTO(
        Long id,
        Long userId,
        String userName,
        Long productId,
        String productName,
        Long variantId,
        String variantSize,
        String variantColor,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        String adminComment,
        LocalDateTime createdAt
) {}