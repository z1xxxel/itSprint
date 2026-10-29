package com.sprint.tms_backend.DTO;

public record PurchaseRequest(
        Long userId,
        Long productId,
        Integer quantity,
        Long variantId
) {}