package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.NewsRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRatingRepository extends JpaRepository<NewsRating, Long> {

    Optional<NewsRating> findByNewsIdAndUserId(Long newsId, Long userId);

    @Query("SELECT r.newsId, AVG(r.value), COUNT(r) FROM NewsRating r " +
            "WHERE r.newsId IN :ids GROUP BY r.newsId")
    List<Object[]> findStatsByNewsIds(@Param("ids") List<Long> ids);

    @Query("SELECT COALESCE(AVG(r.value), 0) FROM NewsRating r WHERE r.newsId = :newsId")
    Double findAverageByNewsId(@Param("newsId") Long newsId);

    long countByNewsId(Long newsId);
}