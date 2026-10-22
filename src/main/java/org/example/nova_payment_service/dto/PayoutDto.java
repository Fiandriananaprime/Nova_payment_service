package org.example.nova_payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutDto(
        UUID id,
        BigDecimal amount,
        PayoutStatus status,
        BigDecimal newBalance,
        PayoutMethodType method,
        Instant createdAt,
        Instant paidAt,
        String failureReason
) {
}
