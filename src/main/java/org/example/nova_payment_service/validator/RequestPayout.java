package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RequestPayout(
        @NotNull(message = "Payout method ID is required")
        UUID payoutMethodId,

        @NotNull(message = "Payout amount is required")
        @DecimalMin(value = "0.0001", message = "Payout amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Payout amount must have at most 15 integer digits and 4 decimal places")
        BigDecimal amount
) {
}
