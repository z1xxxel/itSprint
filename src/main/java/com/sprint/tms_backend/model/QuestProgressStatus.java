package com.sprint.tms_backend.model;

public enum QuestProgressStatus {
    IN_PROGRESS,    // В процессе (сотрудник взял квест)
    PENDING_REVIEW, // На проверке (сотрудник нажал "Готово")
    COMPLETED,      // Выполнено (менеджер подтвердил, монеты начислены)
    REJECTED        // Отклонено (менеджер отклонил, можно переделать)
}