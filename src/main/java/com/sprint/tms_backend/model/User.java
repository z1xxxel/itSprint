package com.sprint.tms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // ПОЛНОСТЬЮ ИСКЛЮЧАЕМ ИЗ JSON, ЧТОБЫ JACKSON НЕ ЛЕЗ В БАЗУ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore
    private Department department;

    // В классе User добавьте поле:
    @Column(nullable = false)
    private Boolean isBlocked = false;

    // Репутация за всё время (не сбрасывается)
    private Double lifetimeRating = 0.0;

    // Контроль месячного лимита
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyEarnedCoins = BigDecimal.ZERO;

    private LocalDateTime lastMonthlyReset;
}