package com.sprint.tms_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record SeasonInfoDTO(
        Long id,
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean isActive,

        // Вкладка 1: Общий зачет
        List<LeaderboardEntryDTO> currentLeaderboard,
        List<SeasonResultDTO> previousSeasonResults,

        // Вкладка 2: Внутри подразделения
        String myDepartmentName,                            // Название моего отдела
        Integer myDepartmentPlace,                          // Место моего отдела среди всех
        List<LeaderboardEntryDTO> myDepartmentLeaderboard,  // Рейтинг сотрудников внутри МОЕГО отдела
        List<DepartmentResultDTO> previousDepartmentResults // Результаты отделов в прошлом сезоне
) {}