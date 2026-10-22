package org.example.nova_payment_service.repository;

import jakarta.persistence.LockModeType;
import org.example.nova_payment_service.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Page<Payout> findBySellerId(UUID sellerId, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.id = :id")
    Optional<Payout> findByIdForUpdate(@Param("id") UUID id);
}
