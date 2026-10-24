package com.sprint.tms_backend.model;

public enum OrderStatus {
    PENDING,   // Ожидает подтверждения менеджером
    APPROVED,  // Подтвержден, в процессе выдачи
    COMPLETED, // Выдан (завершен)
    REJECTED,  // Отклонен (монеты вернулись на баланс)
    CANCELLED  // Отменен самим пользователем (монеты вернулись)
}