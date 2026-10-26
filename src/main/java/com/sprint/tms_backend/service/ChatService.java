package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.Chat;
import com.sprint.tms_backend.model.ChatMessage;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.ChatMessageRepository;
import com.sprint.tms_backend.repository.ChatRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    // === Открыть или создать чат ===
    @Transactional
    public ChatSummaryDTO startChat(StartChatRequest request) {
        if (request.myId().equals(request.otherUserId())) {
            throw new RuntimeException("Нельзя создать чат с самим собой");
        }

        Long smaller = Math.min(request.myId(), request.otherUserId());
        Long larger = Math.max(request.myId(), request.otherUserId());

        Chat chat = chatRepository.findByParticipant1IdAndParticipant2Id(smaller, larger)
                .orElseGet(() -> {
                    User p1 = userRepository.findById(smaller).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
                    User p2 = userRepository.findById(larger).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
                    Chat c = new Chat();
                    c.setParticipant1(p1);
                    c.setParticipant2(p2);
                    return chatRepository.save(c);
                });

        return toSummary(chat, request.myId());
    }

    // === Список моих диалогов ===
    @Transactional(readOnly = true)
    public List<ChatSummaryDTO> getMyChats(Long userId) {
        List<Chat> chats = chatRepository.findAllByUser(userId);
        return chats.stream().map(chat -> toSummary(chat, userId)).toList();
    }

    // === Получить один чат с историей сообщений ===
    @Transactional
    public ChatDetailDTO getChat(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        checkParticipant(chat, userId);

        // Автоматически отмечаем все входящие как прочитанные
        messageRepository.markAsRead(chatId, userId);

        User other = chat.getParticipant1().getId().equals(userId)
                ? chat.getParticipant2() : chat.getParticipant1();

        List<ChatMessageDTO> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId).stream()
                .map(this::toMessageDto).toList();

        return new ChatDetailDTO(chat.getId(), other.getId(), other.getName(), messages);
    }

    // === Отправить сообщение ===
    @Transactional
    public ChatMessageDTO sendMessage(Long chatId, SendChatMessageRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        checkParticipant(chat, request.senderId());

        if (request.text() == null || request.text().trim().isEmpty()) {
            throw new RuntimeException("Сообщение не может быть пустым");
        }

        User sender = userRepository.findById(request.senderId())
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));

        ChatMessage msg = new ChatMessage();
        msg.setChat(chat);
        msg.setSender(sender);
        msg.setText(request.text());
        msg.setAttachmentUrl(request.attachmentUrl());
        messageRepository.save(msg);

        chat.setLastMessageAt(LocalDateTime.now());
        chatRepository.save(chat);

        return toMessageDto(msg);
    }

    // === Удалить сообщение (только свое) ===
    @Transactional
    public void deleteMessage(Long chatId, Long messageId, Long userId) {
        ChatMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        if (!msg.getChat().getId().equals(chatId)) {
            throw new RuntimeException("Сообщение не относится к этому чату");
        }
        if (!msg.getSender().getId().equals(userId)) {
            throw new SecurityException("Можно удалять только свои сообщения");
        }

        messageRepository.delete(msg);
    }

    // === Вспомогательные методы ===
    private void checkParticipant(Chat chat, Long userId) {
        if (!chat.getParticipant1().getId().equals(userId)
                && !chat.getParticipant2().getId().equals(userId)) {
            throw new SecurityException("Вы не участник этого чата");
        }
    }

    private ChatSummaryDTO toSummary(Chat chat, Long currentUserId) {
        User other = chat.getParticipant1().getId().equals(currentUserId)
                ? chat.getParticipant2() : chat.getParticipant1();

        ChatMessage lastMsg = messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId());
        long unread = messageRepository.countByChatIdAndSenderIdNotAndReadAtIsNull(chat.getId(), currentUserId);

        return new ChatSummaryDTO(
                chat.getId(),
                other.getId(),
                other.getName(),
                lastMsg != null ? lastMsg.getText() : "Нет сообщений",
                lastMsg != null ? lastMsg.getCreatedAt() : chat.getCreatedAt(),
                unread
        );
    }

    private ChatMessageDTO toMessageDto(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getSender().getId(),
                msg.getSender().getName(),
                msg.getText(),
                msg.getAttachmentUrl(),
                msg.getCreatedAt(),
                msg.getReadAt()
        );
    }
}