package org.example.nova_payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderPaymentRegistrationRequest(
        @NotBlank
        @Pattern(regexp = "MVOLA|ORANGE_MONEY")
        String provider,

        @NotBlank
        @Size(max = 255)
        String providerReference,

        @Size(max = 255)
        String notificationToken
) {
}
