package com.sprint.tms_backend.DTO;

import java.util.List;

public record CreateTemplateRequest(
        Long creatorId,
        String name,
        String description,
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