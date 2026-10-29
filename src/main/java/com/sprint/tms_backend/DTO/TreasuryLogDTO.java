package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.TreasuryTxType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TreasuryLogDTO(
        Long id,
        TreasuryTxType type,
        String typeLabel,
        boolean incoming,
        BigDecimal amount,
        String description,
        Long actorId,
        String actorName,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {}