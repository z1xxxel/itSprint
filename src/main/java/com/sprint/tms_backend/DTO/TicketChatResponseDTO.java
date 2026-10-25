package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TicketChatResponseDTO(
        Long ticketId,
        String topic,
        String description,
        String creatorName,
        String assigneeName,
        TicketStatus status,
        LocalDateTime createdAt,
        List<MessageDTO> messages
) {
    public record MessageDTO(
            Long id,
            String senderName,
            String text,
            LocalDateTime createdAt
    ) {}
}