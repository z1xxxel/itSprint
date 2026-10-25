package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.ThanksTag;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ThanksFeedResponseDTO(
        Long transactionId,
        String fromUserName,
        String toUserName,
        BigDecimal amount,
        ThanksTag tag, // <--- ENUM
        String comment,
        LocalDateTime createdAt
) {}