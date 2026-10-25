package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "survey_template_options")
@Data
public class SurveyTemplateOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyTemplateQuestion question;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Integer value;
}