package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.TicketStatus;

import java.time.LocalDateTime;

public record SupportTicketDTO(
        Long id,
        String topic,
        String description,
        Long creatorId,
        String creatorName,
        Long assigneeId,
        String assigneeName,
        TicketStatus status,
        Boolean isQuestRequest,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        Integer rating,
        String reviewComment
) {}