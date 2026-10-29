package com.sprint.tms_backend.DTO;

public record SendMessageRequest(Long senderId, String text, String attachmentUrl, Long replyToId) {}