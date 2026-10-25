package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record AchievementProfileDTO(
        String groupCode,      // "EVENT_JOINER"
        String name,           // "Участник"
        String description,    // Общее описание
        String iconUrl,
        boolean isEarned,      // Получена ли хоть одна из уровней
        List<LevelDetail> levels
) {
    public record LevelDetail(
            Integer level,         // 1, 2, 3
            String title,          // "Посетил 5 событий"
            boolean isEarned,      // Получен ли конкретно этот уровень
            LocalDateTime earnedAt // Дата получения (если получен)
    ) {}
}