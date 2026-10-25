package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AchievementResponseDTO(
        Long id,
        String code,
        String name,
        String description,
        String iconUrl,
        BigDecimal rewardCoins, // <--- ДОБАВЛЕНО
        LocalDateTime earnedAt
) {}