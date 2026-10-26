package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record ChatSummaryDTO(
        Long chatId,
        Long otherUserId,
        String otherUserName,
        String lastMessageText,
        LocalDateTime lastMessageAt,
        long unreadCount
) {}