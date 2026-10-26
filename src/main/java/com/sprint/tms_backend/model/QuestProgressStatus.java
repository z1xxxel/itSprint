package com.sprint.tms_backend.model;

public enum QuestProgressStatus {
    IN_PROGRESS,
    PENDING_REVIEW,
    COMPLETED,
    REJECTED,
    CANCELLED // <--- ДОБАВЛЕНО (отмена админом после подтверждения)
}