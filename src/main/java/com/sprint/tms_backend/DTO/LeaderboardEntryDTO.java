package com.sprint.tms_backend.DTO;

public record LeaderboardEntryDTO(
        Integer place,
        Long userId,
        String userName,
        Double rating
) {}