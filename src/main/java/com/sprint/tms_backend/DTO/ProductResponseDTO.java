package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        BigDecimal adjustedPrice,     // = basePrice (оставили для UI)
        Integer discountPercent,
        BigDecimal finalPrice,
        Boolean isOnDiscount,
        LocalDateTime discountValidUntil,
        Integer stock,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Double averageRating,
        Integer reviewCount,
        Boolean hasVariants,
        List<ProductVariantDTO> variants
) {}