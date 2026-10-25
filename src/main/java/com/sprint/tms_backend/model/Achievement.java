package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "achievements")
@Data
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementType type;

    private Integer requiredProgress;

    private String iconUrl;

    // НОВОЕ ПОЛЕ: Сколько монет дается за ачивку (0 или null = без монет)
    @Column(precision = 19, scale = 2)
    private BigDecimal rewardCoins = BigDecimal.ZERO;

    // Группировка для UI (например, "EVENT_JOINER" для всех уровней "Участника")
    @Column(name = "group_code")
    private String groupCode;

    // Уровень (1, 2, 3...)
    private Integer level;
}