package com.sprint.tms_backend.DTO;

import java.math.BigDecimal;

public record AdminDashboardDTO(
        long totalUsers,
        long totalProducts,
        BigDecimal totalCoinsInSystem, // Общая сумма всех балансов

        // Очереди на модерацию (то, что требует внимания админа)
        long pendingOrders,
        long pendingEvents,
        long pendingTickets,
        long pendingQuests
) {}