package org.example.nova_payment_service.dto;


import java.util.UUID;

public record PayoutMethodDto(
        UUID id,
        PayoutMethodType type,
        String label,
        String accountName,
        String maskedAccount,
        boolean isDefault,
        boolean verified
) {}
