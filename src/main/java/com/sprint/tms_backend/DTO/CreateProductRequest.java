package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        String description,
        BigDecimal price,                 // Базовая цена
        Integer priceAdjustmentPercent,   // Наценка/уценка
        Integer stock,
        String imageUrl,
        Long categoryId
) {}