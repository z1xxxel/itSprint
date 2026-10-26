package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.StockWriteOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockWriteOffRepository extends JpaRepository<StockWriteOff, Long> {
}