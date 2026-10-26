package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record PenaltyRequest(
        Long actorId,      // Кто штрафует (Менеджер или Админ)
        Long userId,       // Кого штрафуют
        BigDecimal coins,  // Сколько монет списать
        Double rating,     // Сколько рейтинга списать
        String reason      // Причина
) {}