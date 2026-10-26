package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,             // Цена, которую задал админ
        Integer priceAdjustmentPercent,   // Наценка/уценка (+10, -5)
        BigDecimal adjustedPrice,         // Цена с учетом наценки
        Integer discountPercent,          // Активная скидка (или null)
        BigDecimal finalPrice,            // Итоговая цена
        Boolean isOnDiscount,             // Идет ли акция сейчас
        LocalDateTime discountValidUntil, // Когда закончится скидка
        Integer stock,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Double averageRating,
        Integer reviewCount
) {}