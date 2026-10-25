package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.DepartmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentResultRepository extends JpaRepository<DepartmentResult, Long> {
    List<DepartmentResult> findBySeasonIdOrderByPlaceAsc(Long seasonId);
}