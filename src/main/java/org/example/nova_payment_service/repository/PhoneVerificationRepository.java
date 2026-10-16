package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository
        extends JpaRepository<PhoneVerification, UUID> {

    Optional<PhoneVerification>
    findTopByUserIdAndPhoneAndConsumedFalseOrderByCreatedAtDesc(
            UUID userId, String phone
    );
}