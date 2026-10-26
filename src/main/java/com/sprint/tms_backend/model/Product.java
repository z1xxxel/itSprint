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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type = ProductType.PHYSICAL;

    @Column(nullable = false)
    private Integer stock; // Количество на складе

    private String imageUrl;

    // В классе Product.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Boolean isSpecialOffer = false;
    private Integer discountPercent = 0;
    // В классе Product добавьте поле:
    @Column(nullable = false)
    private Boolean isActive = true;
}