package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record RatingTransactionDTO(
        Long id,
        Double amount,
        String reason,
        LocalDateTime createdAt
) {}