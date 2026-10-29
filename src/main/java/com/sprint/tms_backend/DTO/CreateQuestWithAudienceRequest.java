package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateQuestWithAudienceRequest(
        Long creatorId,
        String title,
        String description,
        BigDecimal rewardCoins,
        Double rewardRating,
        LocalDateTime deadline,
        Boolean isMandatory,
        BigDecimal penaltyCoins,
        Double penaltyRating,
        Boolean allUsers,
        List<Long> departmentIds,
        List<Long> userIds,
        List<Long> excludeUserIds
) {}