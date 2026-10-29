package com.sprint.tms_backend.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum UserRole {

    EMPLOYEE(EnumSet.of(
            Permission.EVENT_CREATE,
            Permission.EVENT_EDIT_OWN,
            Permission.COIN_TRANSFER,
            Permission.TICKET_CREATE,
            Permission.CATALOG_VIEW
    )),

    ORGANIZER(EnumSet.of(
            Permission.EVENT_CREATE,
            Permission.EVENT_EDIT_OWN,
            Permission.EVENT_EDIT_ANY,
            Permission.COIN_TRANSFER,
            Permission.CATALOG_VIEW
    )),

    MANAGER(EnumSet.of(
            Permission.EVENT_CREATE,
            Permission.EVENT_EDIT_OWN,
            Permission.COIN_TRANSFER,
            Permission.COIN_GRANT_SUBORDINATE,
            Permission.CATALOG_VIEW,
            Permission.CATALOG_MANAGE,
            Permission.ORDER_MANAGE,
            Permission.QUEST_MANAGE,
            Permission.QUEST_PARTICIPATE
    )),

    SUPPORT(EnumSet.of(
            Permission.TICKET_MANAGE,
            Permission.TICKET_CREATE,
            Permission.COIN_TRANSFER,
            Permission.CATALOG_VIEW,
            Permission.QUEST_MANAGE,
            Permission.QUEST_PARTICIPATE
    )),

    HR(EnumSet.of(
            Permission.EVENT_APPROVE,
            Permission.EVENT_EDIT_ANY,
            Permission.EVENT_VIEW_ANY,
            Permission.COIN_TRANSFER,
            Permission.USER_MANAGE,
            Permission.ORDER_MANAGE,
            Permission.CATALOG_MANAGE,
            Permission.CATALOG_VIEW,
            Permission.QUEST_MANAGE,
            Permission.QUEST_PARTICIPATE,
            Permission.TICKET_CREATE,
            Permission.NEWS_MANAGE
    )),

    ADMIN(EnumSet.allOf(Permission.class));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}