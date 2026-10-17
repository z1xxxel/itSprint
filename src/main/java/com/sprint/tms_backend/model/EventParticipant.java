package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "event_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Data
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}