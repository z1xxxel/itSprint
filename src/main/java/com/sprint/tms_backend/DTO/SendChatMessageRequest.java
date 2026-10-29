package com.sprint.tms_backend.DTO;

public record SendChatMessageRequest(Long senderId, String text, String attachmentUrl, Long replyToId) {}