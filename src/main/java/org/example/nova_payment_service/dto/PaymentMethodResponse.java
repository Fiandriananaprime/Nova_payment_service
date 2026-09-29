package org.example.nova_payment_service.dto;

import org.example.nova_payment_service.entity.PaymentMethodType;

import java.time.Instant;
import java.util.UUID;

public record PaymentMethodResponse(
    UUID id,
    PaymentMethodType type,
    String provider,

    String label,

    String brand,
    String last4,
    Short expMonth,
    Short expYear,

    String phone,

    boolean isDefault,
    boolean verified,

    Instant createdAt,
    Instant updatedAt
){}
