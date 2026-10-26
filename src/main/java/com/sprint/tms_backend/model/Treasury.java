package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "treasury")
@Data
public class Treasury {
    @Id
    private Long id = 1L; // У нас всегда одна запись

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime lastFundedAt;
}