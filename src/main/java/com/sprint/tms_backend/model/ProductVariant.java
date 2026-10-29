package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "product_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size", "color"}))
@Data
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Пустая строка = нет размера */
    @Column(nullable = false, length = 20)
    private String size = "";

    /** Пустая строка = нет цвета */
    @Column(nullable = false, length = 50)
    private String color = "";

    @Column(nullable = false)
    private Integer stock = 0;
}