package com.sprint.tms_backend.DTO;

public record SendMessageRequest(
        Long senderId,
        String text,
        String attachmentUrl // <-- Ссылка на загруженную картинку
) {}