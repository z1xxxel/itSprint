package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.EventParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<EventParticipant> findByEventId(Long eventId);

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    long countByEventId(Long eventId);

    // Считает, на сколько событий записался пользователь (для ачивок и профиля)
    long countByUserId(Long userId);

    List<EventParticipant> findByUserId(Long userId);
}