package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record ActivityPointDTO(
        String date,
        String dayLabel,
        BigDecimal earned,
        BigDecimal spent
) {}