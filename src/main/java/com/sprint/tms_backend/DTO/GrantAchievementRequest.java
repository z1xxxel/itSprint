package com.sprint.tms_backend.DTO;

public record GrantAchievementRequest(
        Long actorId,
        Long userId,
        String achievementCode // Например: "BEST_EMPLOYEE"
) {}