package org.example.nova_payment_service.dto;

    public record ProviderAccount(
            String provider,
            String msisdn,
            String accountName,
            String status
    ) {}
