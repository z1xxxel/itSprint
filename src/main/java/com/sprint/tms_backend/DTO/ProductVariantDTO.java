package com.sprint.tms_backend.DTO;

public record ProductVariantDTO(
        Long id,
        String size,
        String color,
        Integer stock
) {}