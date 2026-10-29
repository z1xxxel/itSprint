package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
}