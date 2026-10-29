package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record NewsDto(
        Long id,
        String title,
        String content,
        String coverUrl,
        Boolean isPinned,
        Long authorId,
        String authorName,
        Integer views,
        Double averageRating,
        Long ratingsCount,
        Integer myRating,
        LocalDateTime expiresAt,
        Boolean isArchived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}