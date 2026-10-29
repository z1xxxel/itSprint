package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "support_tickets")
@Data
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, length = 1000)
    private String description;

    // ДОБАВЛЕНА ЗАЩИТА
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "department"})
    private User creator;

    // ДОБАВЛЕНА ЗАЩИТА
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "department"})
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    private Integer rating;

    @Column(length = 500)
    private String reviewComment;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TicketMessage> messages;

    // в класс SupportTicket, рядом с description:
    @Column(nullable = false)
    private Boolean isQuestRequest = false;
}