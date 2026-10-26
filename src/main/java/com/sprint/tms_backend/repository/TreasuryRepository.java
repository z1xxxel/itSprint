package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Treasury;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TreasuryRepository extends JpaRepository<Treasury, Long> {

    // Блокировка строки, чтобы два менеджера не списали одновременно больше, чем есть
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Treasury t WHERE t.id = 1")
    Optional<Treasury> findByIdForUpdate();
}