package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.UserRole;
import java.math.BigDecimal;

public record UserListResponseDTO(
        Long id,
        String name,
        String email,
        UserRole role,
        Double currentRating,   // Текущий рейтинг сезона
        Double lifetimeRating,  // Общая репутация
        String level,           // Уровень
        BigDecimal balance,
        Boolean isBlocked
) {}