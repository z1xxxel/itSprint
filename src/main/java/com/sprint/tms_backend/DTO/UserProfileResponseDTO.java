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

        // ⬇️ ДВА РЕЙТИНГА
        Double currentRating,     // Текущий сезон (сбрасывается)
        Double lifetimeRating,    // Репутация (навсегда)
        String level,             // Уровень (считается от lifetimeRating)

        Integer currentPlace,     // Место в текущем сезоне
        Long totalUsers,

        Long departmentId,
        String departmentName,
        Integer departmentPlace,

        long organizedEventsCount,
        long participatedEventsCount,

        List<RatingTransactionDTO> recentRatingTransactions,
        List<CoinTransactionDTO> recentBalanceTransactions,
        List<OrderShortDTO> recentOrders,
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