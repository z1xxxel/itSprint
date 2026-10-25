package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record SeasonResultDTO(
        Integer place,
        String userName,
        Double finalRating,
        BigDecimal rewardCoins
) {}