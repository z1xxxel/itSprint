package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Order;
import com.sprint.tms_backend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, OrderStatus status);
    long countByStatus(OrderStatus status);
}