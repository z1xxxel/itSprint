package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateResponseDTO(
        Long id,
        String name,
        String description,
        String creatorName,
        LocalDateTime createdAt,
        List<QuestionDto> questions
) {
    public record QuestionDto(
            Long id,
            String text,
            List<OptionDto> options
    ) {}

    public record OptionDto(
            Long id,
            String text,
            Integer value
    ) {}
}