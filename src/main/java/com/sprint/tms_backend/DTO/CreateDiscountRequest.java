package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;

public record CreateDiscountRequest(
        Integer percent,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}