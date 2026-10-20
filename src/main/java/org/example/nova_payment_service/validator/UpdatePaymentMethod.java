package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePaymentMethod(
        @Size(max = 100, message = "Le label ne doit pas dépasser 100 caractères")
        String label,

        @Pattern(
                regexp = "^\\+?[0-9]{8,15}$",
                message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres, avec un + facultatif"
        )
        String phone,

        Boolean isDefault
) {}