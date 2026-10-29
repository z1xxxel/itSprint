package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.QuestProgressStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuestResponseDTO(
        Long questId,
        String title,
        String description,
        BigDecimal rewardCoins,
        Double rewardRating,
        LocalDateTime deadline,
        Boolean isMandatory,
        BigDecimal penaltyCoins,
        Double penaltyRating,
        QuestProgressStatus myStatus,
        String adminComment
) {}