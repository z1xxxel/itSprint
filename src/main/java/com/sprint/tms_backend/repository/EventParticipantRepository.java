package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.EventParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<EventParticipant> findByEventId(Long eventId);
}