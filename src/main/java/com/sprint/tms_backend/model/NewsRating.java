package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"news_id", "user_id"}))
@Data
public class NewsRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Оценка 1..5. Хранится приватно — кто поставил, нигде не отображается. */
    @Column(nullable = false)
    private Integer value;

    @CreationTimestamp
    private LocalDateTime createdAt;
}