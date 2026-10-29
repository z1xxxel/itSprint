package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Survey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    // creator eager — чтобы Jackson не падал на LazyInitializationException
    @Override
    @EntityGraph(attributePaths = {"creator"})
    Optional<Survey> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"creator"})
    List<Survey> findAll();

    @EntityGraph(attributePaths = {"creator"})
    List<Survey> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"creator"})
    List<Survey> findByIsActiveTrueAndDeadlineBefore(LocalDateTime now);

    @EntityGraph(attributePaths = {"creator"})
    List<Survey> findByIsActive(Boolean isActive);
}