package com.sprint.tms_backend.DTO;

public record CreateProductVariantRequest(
        String size,
        String color,
        Integer stock
) {}