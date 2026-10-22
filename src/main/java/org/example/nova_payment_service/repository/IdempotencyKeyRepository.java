package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findBySellerIdAndIdempotencyKey(UUID sellerId, UUID idempotencyKey);
}
