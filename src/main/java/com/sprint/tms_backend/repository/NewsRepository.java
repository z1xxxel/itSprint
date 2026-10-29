package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.News;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<News> findByIsArchivedFalseOrderByIsPinnedDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"author"})
    List<News> findByIsArchivedTrueOrderByArchivedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<News> findById(Long id);

    /** ID всех активных новостей, у которых истёк срок — для планировщика */
    @Query("SELECT n.id FROM News n WHERE n.isArchived = false " +
            "AND n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<Long> findIdsToArchive(@Param("now") LocalDateTime now);
}