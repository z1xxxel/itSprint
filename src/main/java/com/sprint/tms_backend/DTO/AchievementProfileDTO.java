package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AchievementProfileDTO(
        String groupCode,
        String name,
        String description,
        String iconUrl,
        boolean isEarned,
        List<LevelDetail> levels
) {
    public record LevelDetail(
            Integer level,
            String title,
            String condition,        // "Посетил 5 корпоративных событий"
            Integer requiredProgress,// 5 / 10 / 20 (null для ручных)
            BigDecimal rewardCoins,  // 200 / 500 / 1000
            boolean isEarned,
            LocalDateTime earnedAt
    ) {}
}