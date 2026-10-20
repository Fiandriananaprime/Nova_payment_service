package org.example.nova_payment_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentMethodResponse(
        UUID id,

        PaymentMethodType type,

        @Size(max = 100)
        String provider,

        @Size(max = 255)
        String providerPaymentMethodId,

        @Size(max = 100)
        String label,

        @Pattern(regexp = "^[a-zA-Z ]+$")
        @Size(max = 50)
        String brand,

        @Pattern(regexp = "^\\d{4}$")
        String last4,

        @Min(1)
        @Max(12)
        Short expMonth,

        @Min(2026)
        @Max(2100)
        Short expYear,

        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phone,

        boolean isDefault,

        boolean verified,

        Instant createdAt,

        Instant updatedAt
){}
