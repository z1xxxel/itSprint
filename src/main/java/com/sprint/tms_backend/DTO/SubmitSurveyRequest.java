package com.sprint.tms_backend.DTO;

import java.util.Map;

public record SubmitSurveyRequest(
        Long userId,
        Map<Long, Long> answers // Ключ: ID вопроса, Значение: ID выбранного варианта
) {}