package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.QuestProgressStatus;

import java.math.BigDecimal;

public record QuestResponseDTO(
        Long questId,
        String title,
        String description,
        BigDecimal rewardCoins,
        QuestProgressStatus myStatus, // null, если еще не брал
        String adminComment
) {}