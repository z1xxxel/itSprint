package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fromTime;

    @Column(nullable = false)
    private LocalDateTime toTime;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String nameEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column
    private String description;

    @Column
    private String eventPlace;

    @Column
    private String commentToAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "Varchar(20)")
    private EventStatus eventStatus;
}
