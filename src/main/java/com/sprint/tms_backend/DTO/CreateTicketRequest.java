package com.sprint.tms_backend.DTO;

public record CreateTicketRequest(
        Long userId,
        String topic,
        String description,
        Boolean isQuestRequest
) {}