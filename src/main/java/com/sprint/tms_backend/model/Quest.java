package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quests")
@Data
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal rewardCoins;

    /** Рейтинговая награда (0-5) */
    @Column
    private Double rewardRating;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private LocalDateTime deadline;

    @Column(nullable = false)
    private Boolean isMandatory = false;

    @Column(precision = 19, scale = 2)
    private BigDecimal penaltyCoins;

    @Column
    private Double penaltyRating;

    @Column(nullable = false)
    private Boolean isPublic = true;

    @Column(length = 1000)
    private String audienceDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "department"})
    private User creator;

    @CreationTimestamp
    private LocalDateTime createdAt;
}