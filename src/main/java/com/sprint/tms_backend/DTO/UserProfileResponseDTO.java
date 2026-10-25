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

        // Место в общем рейтинге
        Integer currentPlace,
        Long totalUsers,

        // Информация о подразделении
        Long departmentId,        // <--- ДОБАВЛЕНО
        String departmentName,    // <--- ДОБАВЛЕНО
        Integer departmentPlace,  // <--- ДОБАВЛЕНО

        // Статистика
        long organizedEventsCount,
        long participatedEventsCount,

        // История (для графиков и блока последних изменений)
        List<RatingTransactionDTO> recentRatingTransactions,
        List<CoinTransactionDTO> recentBalanceTransactions,

        // Последние заказы
        List<OrderShortDTO> recentOrders,

        // Сгруппированные ачивки
        List<AchievementProfileDTO> achievements
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