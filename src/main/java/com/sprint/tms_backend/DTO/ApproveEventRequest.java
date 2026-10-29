package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record ApproveEventRequest(
        Long actorId,
        Boolean paidInCoins,
        BigDecimal rewardCoins,
        Double rewardRating
) {}