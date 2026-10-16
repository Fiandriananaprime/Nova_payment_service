package org.example.nova_payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.nova_payment_service.entity.PaymentMethodType;

public record CreatePaymentMethod(
    @NotNull(message = "Le type de paiement est obligatoire")
        PaymentMethodType type,

    @Size(max = 100, message = "Le label ne doit pas dépasser 100 caractères")
    String label,

    @Size(max = 100, message = "Le provider ne doit pas dépasser 100 caractères")
    String provider,

    @Size(max = 255, message = "L'identifiant du moyen de paiement ne doit pas dépasser 255 caractères")
    String providerPaymentMethodId,

    @Pattern(
            regexp = "^\\+?[0-9]{8,15}$",
            message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres, avec un + facultatif"
    )
    String phone,

    Boolean isDefault
    ) {}
