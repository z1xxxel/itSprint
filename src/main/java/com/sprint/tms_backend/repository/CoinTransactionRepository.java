package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.CoinTransaction;
import com.sprint.tms_backend.model.CoinTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    // История операций пользователя (входящие + исходящие)
    @Query("SELECT t FROM CoinTransaction t " +
            "WHERE t.fromUserId = :userId OR t.toUserId = :userId " +
            "ORDER BY t.createdAt DESC")
    Page<CoinTransaction> findHistoryByUser(@Param("userId") Long userId, Pageable pageable);

    // Сумма, отправленная от fromId к toId с определённого момента (для лимита peer-to-peer)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoinTransaction t " +
            "WHERE t.fromUserId = :fromId AND t.toUserId = :toId " +
            "AND t.type = :type AND t.createdAt >= :since")
    BigDecimal sumSentBetween(
            @Param("fromId") Long fromId,
            @Param("toId") Long toId,
            @Param("type") CoinTransactionType type,
            @Param("since") LocalDateTime since);

    // Сумма, отправленная от fromId любого получателю с определённого момента
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoinTransaction t " +
            "WHERE t.fromUserId = :fromId " +
            "AND t.type = :type AND t.createdAt >= :since")
    BigDecimal sumSentTotal(
            @Param("fromId") Long fromId,
            @Param("type") CoinTransactionType type,
            @Param("since") LocalDateTime since);

    Page<CoinTransaction> findByTypeOrderByCreatedAtDesc(CoinTransactionType type, Pageable pageable);
}