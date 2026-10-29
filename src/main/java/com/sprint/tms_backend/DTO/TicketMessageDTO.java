package com.sprint.tms_backend.DTO;
import java.time.LocalDateTime;
public record TicketMessageDTO(
        Long id, Long ticketId, Long senderId, String senderName,
        String text, String attachmentUrl, Long replyToId,
        LocalDateTime editedAt, LocalDateTime createdAt
) {}