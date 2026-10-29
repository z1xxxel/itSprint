package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record UpdateNewsRequest(
        String title,
        String content,
        String coverUrl,
        Boolean isPinned,
        LocalDateTime expiresAt
) {}