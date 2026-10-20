package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.AssertTrue;

public record PayoutUpdate(
        String label,
        Boolean verify,
        Boolean isDefault
) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean isAtLeastOneFieldProvided() {
        return label != null || verify != null || isDefault != null;
    }
}