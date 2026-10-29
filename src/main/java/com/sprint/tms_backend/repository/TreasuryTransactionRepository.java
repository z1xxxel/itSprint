package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.TreasuryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TreasuryTransactionRepository extends JpaRepository<TreasuryTransaction, Long> {
    Page<TreasuryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}