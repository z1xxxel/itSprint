package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record UserQuestForReview(
        Long id,
        Long userId,
        String questTitle,
        String userName,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        String adminComment
) {}