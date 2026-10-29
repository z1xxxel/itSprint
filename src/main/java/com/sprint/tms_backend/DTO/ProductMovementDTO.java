package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record ProductMovementDTO(
        Long id,
        String action,
        String actorName,
        Integer quantityDelta,
        String details,
        LocalDateTime createdAt
) {}