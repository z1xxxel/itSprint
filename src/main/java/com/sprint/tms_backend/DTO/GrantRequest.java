package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record GrantRequest(
        Long actorId,
        Long toUserId,
        BigDecimal amount,
        String comment
) {}