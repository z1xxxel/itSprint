package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Например: "Мерч", "Обучение", "Отгулы"

    private String description; // Краткое описание для блока на сайте

    private String imageUrl; // Картинка/иконка для блока категории

    private Integer sortOrder = 0; // Чтобы менеджер мог двигать блоки вверх/вниз на сайте
}