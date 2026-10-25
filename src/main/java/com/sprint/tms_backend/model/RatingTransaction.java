package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rating_transactions")
@Data
public class RatingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount; // Может быть +5.0 или -1.5

    @Column(nullable = false)
    private String reason; // "Прохождение опроса", "Награда за событие"

    @CreationTimestamp
    private LocalDateTime createdAt;
}