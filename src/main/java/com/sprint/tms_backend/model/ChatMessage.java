package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_msg_chat", columnList = "chat_id"),
        @Index(name = "idx_chat_msg_created", columnList = "created_at")
})
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    @JsonIgnore
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User sender;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(length = 500)
    private String attachmentUrl;

    /** ID сообщения, на которое это является ответом */
    private Long replyToId;

    /** Дата редактирования (null = не редактировалось) */
    private LocalDateTime editedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}