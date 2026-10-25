package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record CreateQuestRequest(
        Long creatorId,
        String title,
        String description,
        BigDecimal rewardCoins
) {}