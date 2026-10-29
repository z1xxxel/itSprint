package com.sprint.tms_backend.DTO;
import java.time.LocalDateTime;
public record ChatMessageDTO(
        Long id, Long senderId, String senderName, String text,
        String attachmentUrl, Long replyToId, LocalDateTime editedAt,
        LocalDateTime createdAt, LocalDateTime readAt
) {}