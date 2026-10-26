package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        Long categoryId,
        Boolean isSpecialOffer,   // <--- ДОБАВЛЕНО
        Integer discountPercent   // <--- ДОБАВЛЕНО
) {}