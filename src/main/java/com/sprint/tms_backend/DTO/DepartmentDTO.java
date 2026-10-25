package com.sprint.tms_backend.DTO;

public record DepartmentDTO(
        Long id,
        String name,
        String description,
        long memberCount,
        Double totalRating
) {}