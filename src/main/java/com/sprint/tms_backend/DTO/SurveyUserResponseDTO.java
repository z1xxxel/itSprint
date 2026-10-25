package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyUserResponseDTO(
        Long userId,
        String userName,
        String userEmail,
        LocalDateTime answeredAt,
        List<AnswerDetailDTO> answers
) {
    public record AnswerDetailDTO(
            String questionText,
            String optionText,
            Integer value
    ) {}
}