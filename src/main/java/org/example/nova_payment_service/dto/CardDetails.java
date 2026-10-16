package org.example.nova_payment_service.dto;

public record CardDetails(
        String paymentMethodId,
        String brand,
        String last4,
        Long expMonth,
        Long expYear
){}
