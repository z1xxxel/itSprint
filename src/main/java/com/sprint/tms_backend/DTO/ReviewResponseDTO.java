package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record ReviewResponseDTO(
        Long id,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}