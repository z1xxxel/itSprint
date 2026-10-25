package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record CreateSurveyRequest(
        Long creatorId,
        String title,
        String description,
        LocalDateTime deadline, // <--- НОВОЕ
        Double rewardRating,    // <--- НОВОЕ
        Double penaltyRating,   // <--- НОВОЕ
        List<QuestionDto> questions
) {
    public record QuestionDto(
            String text,
            List<OptionDto> options
    ) {}

    public record OptionDto(
            String text,
            Integer value
    ) {}
}