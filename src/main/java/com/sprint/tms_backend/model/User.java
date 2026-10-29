package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private Double rating = 0.0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore
    private Department department;

    @Column(nullable = false)
    private Boolean isBlocked = false;

    private Double lifetimeRating = 0.0;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyEarnedCoins = BigDecimal.ZERO;

    private LocalDateTime lastMonthlyReset;

    @Column(length = 500)
    private String avatarUrl;

    // === STREAK ===

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer longestStreak = 0;

    @Column
    private LocalDate lastActiveDate;

    /** На каком значении streak последний раз давали награду */
    @Column(nullable = false)
    private Integer lastStreakRewardAt = 0;
}