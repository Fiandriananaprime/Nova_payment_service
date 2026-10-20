package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
}
