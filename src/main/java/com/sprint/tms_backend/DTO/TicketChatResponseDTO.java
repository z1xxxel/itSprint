package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TicketChatResponseDTO(
        Long ticketId,
        Long creatorId,
        Long assigneeId,
        String topic,
        String description,
        String creatorName,
        String assigneeName,
        TicketStatus status,
        Boolean isQuestRequest,
        LocalDateTime createdAt,
        Integer rating,
        String reviewComment,
        List<MessageDTO> messages
) {
    public record MessageDTO(
            Long id,
            Long senderId,
            String senderName,
            String text,
            String attachmentUrl,
            Long replyToId,
            LocalDateTime editedAt,
            LocalDateTime createdAt
    ) {}
}