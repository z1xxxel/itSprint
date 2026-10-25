package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.OrderStatus;
import com.sprint.tms_backend.model.UserRole;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileResponseDTO(
        Long id,
        String name,
        String email,
        UserRole role,
        BigDecimal balance,
        Double rating,
        String level,

        long organizedEventsCount,
        long participatedEventsCount,

        List<CoinTransactionDTO> recentTransactions,
        List<OrderShortDTO> recentOrders,
        List<AchievementResponseDTO> achievements // <--- ТЕПЕРЬ ВНУТРИ
) {
    public record OrderShortDTO(
            Long orderId,
            String productName,
            Integer quantity,
            BigDecimal totalPrice,
            OrderStatus status,
            String adminComment
    ) {}
}