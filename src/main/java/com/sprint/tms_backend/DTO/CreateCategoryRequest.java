package com.sprint.tms_backend.DTO;

public record CreateCategoryRequest(
        String name,
        String description,
        String imageUrl,
        Integer sortOrder
) {}