package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);

    ChatMessage findFirstByChatIdOrderByCreatedAtDesc(Long chatId);

    long countByChatIdAndSenderIdNotAndReadAtIsNull(Long chatId, Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readAt = CURRENT_TIMESTAMP " +
            "WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.readAt IS NULL")
    int markAsRead(@Param("chatId") Long chatId, @Param("userId") Long userId);
}