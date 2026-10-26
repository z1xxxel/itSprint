package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal finalPrice, // Цена со скидкой
        Boolean isSpecialOffer,
        Integer stock,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Double averageRating, // Средний рейтинг
        Integer reviewCount   // Кол-во отзывов
) {}