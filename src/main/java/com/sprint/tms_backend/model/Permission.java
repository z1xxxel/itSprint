package com.sprint.tms_backend.model;

public enum Permission {
    // События
    EVENT_CREATE,
    EVENT_EDIT_OWN,
    EVENT_EDIT_ANY,
    EVENT_APPROVE,
    EVENT_VIEW_ANY,

    // Монеты
    COIN_TRANSFER,
    COIN_GRANT_SUBORDINATE,
    COIN_GRANT_ANY,

    // Каталог (на будущее)
    CATALOG_VIEW,
    CATALOG_MANAGE,

    // Пользователи
    USER_MANAGE,
    ROLE_ASSIGN
}