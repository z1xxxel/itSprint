package com.sprint.tms_backend.DTO;

public record ParticipantResponseDTO(
        Long userId,
        String name,
        String email
) {}