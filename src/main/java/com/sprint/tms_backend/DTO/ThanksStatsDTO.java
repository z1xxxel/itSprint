package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.util.List;

public record ThanksStatsDTO(
        long totalReceived,
        long totalSent,
        BigDecimal totalCoinsReceived,
        BigDecimal totalCoinsSent,
        List<TopThanker> topThankers
) {
    public record TopThanker(String userName, long count) {}
}