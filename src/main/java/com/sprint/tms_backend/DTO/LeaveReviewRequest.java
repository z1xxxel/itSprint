package com.sprint.tms_backend.DTO;

public record LeaveReviewRequest(
        Long userId,
        Integer rating, // 1-5
        String comment
) {}