package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(nullable = false, length = 20)
    private EventStatus eventStatus;

    // Награда
    @Column(nullable = false)
    private Boolean paidInCoins = false;

    @Column(precision = 19, scale = 2)
    private BigDecimal rewardCoins;

    @Column
    private Double rewardRating;

    @Column(nullable = false)
    private Boolean rewardsGranted = false;
}