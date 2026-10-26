package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // Открыть диалог с пользователем (создаст, если не существует)
    @PostMapping("/start")
    public ResponseEntity<ChatSummaryDTO> startChat(@RequestBody StartChatRequest request) {
        return new ResponseEntity<>(chatService.startChat(request), HttpStatus.OK);
    }

    // Список моих диалогов
    @GetMapping("/my")
    public ResponseEntity<List<ChatSummaryDTO>> getMyChats(@RequestParam Long userId) {
        return ResponseEntity.ok(chatService.getMyChats(userId));
    }

    // Открыть чат (заодно пометит сообщения прочитанными)
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDetailDTO> getChat(
            @PathVariable Long chatId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(chatService.getChat(chatId, userId));
    }

    // Отправить сообщение
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(chatId, request));
    }

    // Удалить сообщение
    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        chatService.deleteMessage(chatId, messageId, userId);
        return ResponseEntity.ok("Сообщение удалено");
    }
}