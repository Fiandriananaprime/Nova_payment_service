package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Page<Payout> findBySellerId(UUID sellerId, Pageable page);
}
