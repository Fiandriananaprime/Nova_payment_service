package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MvolaCallbackRequest(
        @NotBlank
        @Pattern(regexp = "completed|failed")
        String transactionStatus,

        @NotBlank
        @Size(max = 255)
        String serverCorrelationId,

        @Size(max = 255)
        String transactionReference
) {
}
