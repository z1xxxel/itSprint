package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.QuestProgressStatus;

import java.time.LocalDateTime;

public record AdminQuestDTO(
        Long userQuestId,
        String questTitle,
        String userName,
        QuestProgressStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        String adminComment
) {}