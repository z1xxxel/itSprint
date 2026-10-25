package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByIsActiveTrue();

    // Для планировщика: ищем активные сезоны с истекшим сроком
    List<Season> findByIsActiveTrueAndEndDateBefore(LocalDateTime now);

    // Для истории (закрытые сезоны)
    List<Season> findByIsActiveFalseOrderByEndDateDesc();
}