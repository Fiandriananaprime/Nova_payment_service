package org.example.nova_payment_service.dto;

public enum IdempotencyKeyStatus {
    COMPLETED,
    PROCESSING,
    FAILED
}
