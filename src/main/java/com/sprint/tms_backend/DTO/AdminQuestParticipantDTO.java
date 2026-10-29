package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.QuestProgressStatus;

import java.time.LocalDateTime;

public record AdminQuestParticipantDTO(
        Long userQuestId,
        Long userId,
        String userName,
        QuestProgressStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt
) {}