package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByIsActiveTrue();

    // Ищем активные опросы, у которых дедлайн уже прошел
    List<Survey> findByIsActiveTrueAndDeadlineBefore(LocalDateTime now);

    List<Survey> findByIsActive(Boolean isActive);
}