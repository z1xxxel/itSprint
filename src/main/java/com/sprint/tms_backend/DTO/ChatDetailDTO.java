package com.sprint.tms_backend.DTO;

import java.util.List;

public record ChatDetailDTO(
        Long chatId,
        Long otherUserId,
        String otherUserName,
        List<ChatMessageDTO> messages
) {}