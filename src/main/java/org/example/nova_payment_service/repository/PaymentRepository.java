package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findAllByUserId(UUID userId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id,UUID userId);

    long deleteByUserIdAndId(UUID userId, UUID paymentMethodId);
}
