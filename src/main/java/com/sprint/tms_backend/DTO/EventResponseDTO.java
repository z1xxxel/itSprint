package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponseDTO(
        Long id,
        Long organizerId,
        String organizerName,
        LocalDateTime fromTime,
        LocalDateTime toTime,
        String category,
        String nameEvent,
        String description,
        String eventPlace,
        String commentToAdmin,
        EventStatus eventStatus,
        Boolean paidInCoins,
        BigDecimal rewardCoins,
        Double rewardRating,
        Boolean rewardsGranted
) {}