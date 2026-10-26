package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.ThanksTag;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserThanksDTO(
        Long transactionId,
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        BigDecimal amount,
        ThanksTag tag,
        String comment,
        LocalDateTime createdAt
) {}