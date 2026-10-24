package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.CoinTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CoinTransactionDTO(
        Long id,
        Long fromUserId,
        Long toUserId,
        BigDecimal amount,
        CoinTransactionType type,
        String comment,
        LocalDateTime createdAt
) {}