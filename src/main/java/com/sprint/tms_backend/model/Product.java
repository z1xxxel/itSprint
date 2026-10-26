package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    // БАЗОВАЯ цена (то, что задает админ). Без скидок и наценок.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    // Постоянная наценка/уценка в процентах. Может быть отрицательной.
    // Например: -10 (уценка 10%), 0 (без изменений), +15 (наценка 15%)
    @Column(nullable = false)
    private Integer priceAdjustmentPercent = 0;

    @Column(nullable = false)
    private Integer stock;

    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type = ProductType.PHYSICAL;
}