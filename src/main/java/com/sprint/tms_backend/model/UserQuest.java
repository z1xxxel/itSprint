package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_quests", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
@Data
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "department"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Quest quest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestProgressStatus status;

    @CreationTimestamp
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    @Column(length = 500)
    private String adminComment; // Причина отклонения
}