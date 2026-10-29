package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_messages")
@Data
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(length = 500)
    private String attachmentUrl;

    private Long replyToId;
    private LocalDateTime editedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}