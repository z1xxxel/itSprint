package com.sprint.tms_backend.DTO;

public record UpdateUserRequest(
        String name,
        String email,
        String password
) {}