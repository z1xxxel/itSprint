package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.SeasonResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonResultRepository extends JpaRepository<SeasonResult, Long> {
    List<SeasonResult> findBySeasonIdOrderByPlaceAsc(Long seasonId);
}