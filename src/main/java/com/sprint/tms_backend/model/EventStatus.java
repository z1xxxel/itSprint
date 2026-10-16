package com.sprint.tms_backend.model;

public enum EventStatus {
    NEW,
    ASSIGNED,       // Перевозчик назначен, но еще не подтвердил
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
