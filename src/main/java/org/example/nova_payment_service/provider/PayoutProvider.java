package org.example.nova_payment_service.provider;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutProvider {
    void validateConfiguration();

    PayoutResult execute(
            BigDecimal amount,
            String recipientMsisdn,
            String reference,
            String idempotencyKey,
            UUID payoutId
    );

    record PayoutResult(
            String status,
            String providerReference,
            String failureReason
    ) {
        public boolean completed() {
            return "COMPLETED".equalsIgnoreCase(status);
        }

        public boolean failed() {
            return "FAILED".equalsIgnoreCase(status);
        }
    }
}
