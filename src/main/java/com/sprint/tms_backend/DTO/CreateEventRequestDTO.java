package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateEventRequestDTO(
        Long organizerId,
        LocalDateTime fromTime,
        LocalDateTime toTime,
        String category,
        String nameEvent,
        String description,
        String eventPlace,
        String commentToAdmin,
        Boolean paidInCoins,
        BigDecimal rewardCoins,
        Double rewardRating
) {}