package com.sprint.tms_backend.DTO;

public record WriteOffStockRequest(
        Integer quantity,
        String reason,        // DAMAGED | GIFT | LOST | OTHER
        Long recipientId,     // Опционально (для подарков)
        String comment
) {}