package org.example.nova_payment_service.provider;

import java.math.BigDecimal;

public interface PayoutProvider {
    PayoutResult execute(
            BigDecimal amount,
            String recipientMsisdn,
            String reference,
            String idempotencyKey
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
