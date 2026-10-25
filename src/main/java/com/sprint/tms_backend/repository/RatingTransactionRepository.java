package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.RatingTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingTransactionRepository extends JpaRepository<RatingTransaction, Long> {

    // Для получения последних N изменений рейтинга пользователя
    Page<RatingTransaction> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}