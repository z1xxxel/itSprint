package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.QuestProgressStatus;
import com.sprint.tms_backend.model.UserQuest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByUserIdAndQuestId(Long userId, Long questId);

    List<UserQuest> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"quest", "user"})
    List<UserQuest> findByStatus(QuestProgressStatus status);

    @EntityGraph(attributePaths = {"quest", "user"})
    List<UserQuest> findByQuestId(Long questId);

    long countByStatus(QuestProgressStatus status);
}