package com.sprint.tms_backend.DTO;

public record TopThankedDTO(
        Long userId,
        String userName,
        String avatarUrl,
        Long count
) {}