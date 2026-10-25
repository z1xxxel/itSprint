package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"organizer"})
    List<Event> findByOrganizerId(Long organizerId);

    @EntityGraph(attributePaths = {"organizer"})
    List<Event> findByEventStatusNot(EventStatus status);

    @EntityGraph(attributePaths = {"organizer"})
    List<Event> findByOrganizerIdAndEventStatusNot(Long organizerId, EventStatus status);

    @Override
    @EntityGraph(attributePaths = {"organizer"})
    List<Event> findAll();

    @Query("SELECT e.id FROM Event e " +
            "WHERE e.eventStatus = :assigned AND e.toTime < :now")
    List<Long> findIdsToComplete(
            @Param("assigned") EventStatus assigned,
            @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // Считает, сколько событий организовал пользователь (для профиля)
    long countByOrganizerId(Long organizerId);
}