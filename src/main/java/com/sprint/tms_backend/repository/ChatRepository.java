package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByParticipant1IdAndParticipant2Id(Long p1, Long p2);

    @Query("SELECT c FROM Chat c WHERE c.participant1.id = :userId OR c.participant2.id = :userId " +
            "ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC")
    List<Chat> findAllByUser(@Param("userId") Long userId);
}