package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    // Найти активную скидку на товар в данный момент времени
    @Query("SELECT d FROM Discount d WHERE d.product.id = :productId " +
            "AND d.isActive = true AND :now BETWEEN d.validFrom AND d.validTo")
    Optional<Discount> findActiveDiscount(@Param("productId") Long productId,
                                          @Param("now") LocalDateTime now);

    // Проверить, есть ли уже активная скидка на товар (для валидации при создании)
    @Query("SELECT COUNT(d) > 0 FROM Discount d WHERE d.product.id = :productId " +
            "AND d.isActive = true AND :now BETWEEN d.validFrom AND d.validTo")
    boolean existsActiveDiscount(@Param("productId") Long productId,
                                 @Param("now") LocalDateTime now);
}