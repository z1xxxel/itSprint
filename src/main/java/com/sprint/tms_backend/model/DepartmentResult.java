package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "department_results")
@Data
public class DepartmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private Long departmentId;

    @Column(nullable = false)
    private String departmentName;

    @Column(nullable = false)
    private Integer place; // 1, 2, 3...

    @Column(nullable = false)
    private Double totalRating; // Сумма рейтингов всех сотрудников

    @Column(nullable = false)
    private Integer memberCount;

    @Column(precision = 19, scale = 2)
    private BigDecimal rewardCoinsPerMember; // Сколько получил каждый участник

    @CreationTimestamp
    private LocalDateTime createdAt;
}