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
import java.util.List;

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
    // Глобальная история для админки
    Page<CoinTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Все благодарности, где пользователь — получатель
    @Query("SELECT t FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.toUserId = :userId " +
            "ORDER BY t.createdAt DESC")
    Page<CoinTransaction> findReceivedThanks(@Param("userId") Long userId, Pageable pageable);

    // Все благодарности, где пользователь — отправитель
    @Query("SELECT t FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.fromUserId = :userId " +
            "ORDER BY t.createdAt DESC")
    Page<CoinTransaction> findSentThanks(@Param("userId") Long userId, Pageable pageable);

    // Сколько всего получено / отправлено
    @Query("SELECT COUNT(t) FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.toUserId = :userId")
    long countReceived(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.fromUserId = :userId")
    long countSent(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.toUserId = :userId")
    BigDecimal sumReceived(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.fromUserId = :userId")
    BigDecimal sumSent(@Param("userId") Long userId);

    // Топ тех, кто благодарил пользователя
    @Query("SELECT t.fromUserId, COUNT(t) as cnt FROM CoinTransaction t " +
            "WHERE t.type = 'PEER_THANKS' AND t.toUserId = :userId " +
            "GROUP BY t.fromUserId ORDER BY cnt DESC")
    List<Object[]> findTopThankers(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM CoinTransaction t WHERE t.type = 'PEER_THANKS' " +
            "AND (t.fromUserId = :userId OR t.toUserId = :userId) " +
            "ORDER BY t.createdAt DESC")
    Page<CoinTransaction> findAllThanksByUser(@Param("userId") Long userId, Pageable pageable);
}