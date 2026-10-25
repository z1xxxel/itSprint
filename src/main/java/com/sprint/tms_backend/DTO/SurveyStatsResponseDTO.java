package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyStatsResponseDTO(
        Long surveyId,
        String title,
        LocalDateTime deadline,
        boolean isActive,

        // Общая статистика
        long totalUsers,           // Всего пользователей в системе
        long respondedUsers,       // Сколько прошло опрос
        double responseRatePercent, // Процент прохождения

        // Детализация по вопросам
        List<QuestionStatsDTO> questions
) {
    public record QuestionStatsDTO(
            Long questionId,
            String questionText,
            long totalAnswers,      // Сколько всего ответов на этот вопрос
            double averageValue,    // Средний балл (например, 4.2)
            List<OptionStatsDTO> options
    ) {}

    public record OptionStatsDTO(
            Long optionId,
            String optionText,
            Integer value,
            long count,             // Сколько раз выбрали
            double percentage       // Процент от ответивших на этот вопрос
    ) {}
}