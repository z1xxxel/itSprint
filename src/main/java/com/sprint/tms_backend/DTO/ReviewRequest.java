package com.sprint.tms_backend.DTO;

public record ReviewRequest(
        Long userId,
        Integer rating, // 1-5
        String comment
) {}