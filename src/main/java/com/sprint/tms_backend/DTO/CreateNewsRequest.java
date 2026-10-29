package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record CreateNewsRequest(
        String title,
        String content,
        String coverUrl,
        Boolean isPinned,
        LocalDateTime expiresAt
) {}