package com.sprint.tms_backend.DTO;

public record LeaderboardEntryDTO(
        Integer place,
        Long userId,
        String userName,
        Double currentRating,   // Рейтинг в текущем сезоне (для лидерборда)
        Double lifetimeRating   // Общая репутация (для доп. инфо)
) {}