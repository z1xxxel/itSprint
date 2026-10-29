package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.UserRole;
import java.math.BigDecimal;

public record UserListResponseDTO(
        Long id,
        String name,
        String email,
        UserRole role,
        Double currentRating,
        Double lifetimeRating,
        String level,
        BigDecimal balance,
        Boolean isBlocked,
        Long departmentId,
        String departmentName
) {}