package com.sprint.tms_backend.DTO;

public record DepartmentLeaderboardEntryDTO(
        Integer place,
        Long departmentId,
        String departmentName,
        Double totalRating,
        Integer memberCount
) {}