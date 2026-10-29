package com.sprint.tms_backend.DTO;

import java.util.List;

public record ApplyQuestPenaltyRequest(
        Long actorId,
        List<Long> userIds,
        Boolean applyCoins,
        Boolean applyRating,
        String reason
) {}