package org.example.nova_payment_service.dto;

import jakarta.validation.constraints.*;
import org.example.nova_payment_service.entity.PaymentMethodType;

public record CreatePaymentMethod (
    @NotNull
    PaymentMethodType type,

    @Size(max = 100)
    String provider,

    @Size(max = 255)
    String providerPaymentMethodId,

    @Size(max = 100)
    String label,

    @Size(max = 50)
    String brand,

    @Pattern(regexp = "\\d{4}")
    String last4,

    @Min(1)
    @Max(12)
    Short expMonth,

    @Min(2000)
    @Max(2500)
    Short expYear,

    @Size(max = 30)
    String phone,

    Boolean isDefault
){}
