package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.SurveyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyTemplateRepository extends JpaRepository<SurveyTemplate, Long> {
    boolean existsByName(String name);
}