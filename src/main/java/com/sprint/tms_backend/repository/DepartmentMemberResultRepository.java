package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.DepartmentMemberResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentMemberResultRepository extends JpaRepository<DepartmentMemberResult, Long> {
    List<DepartmentMemberResult> findBySeasonIdAndDepartmentIdOrderByPlaceInDepartmentAsc(Long seasonId, Long departmentId);
}