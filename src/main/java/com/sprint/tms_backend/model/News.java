package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news", indexes = {
        @Index(name = "idx_news_created", columnList = "created_at"),
        @Index(name = "idx_news_pinned", columnList = "is_pinned"),
        @Index(name = "idx_news_archived", columnList = "is_archived"),
        @Index(name = "idx_news_expires", columnList = "expires_at")
})
@Data
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 500)
    private String coverUrl;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    /** Срок актуальности. null = без срока (не автоархивируется). */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** В архиве или активна. */
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    /** Когда ушла в архив (авто или вручную). */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(nullable = false)
    private Integer views = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}