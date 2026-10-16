package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres")
                String code
) {}

