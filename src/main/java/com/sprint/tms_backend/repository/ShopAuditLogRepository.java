package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.ShopAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopAuditLogRepository extends JpaRepository<ShopAuditLog, Long> {
    Page<ShopAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<ShopAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}