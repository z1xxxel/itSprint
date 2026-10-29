package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateQuestRequest(
        Long actorId,
        String title,
        String description,
        BigDecimal rewardCoins,
        Double rewardRating,
        LocalDateTime deadline,
        BigDecimal penaltyCoins,
        Double penaltyRating
) {}