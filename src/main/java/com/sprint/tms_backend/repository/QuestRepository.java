package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findByIsActiveTrue();
    List<Quest> findByIsActiveTrueOrderByCreatedAtDesc();
}