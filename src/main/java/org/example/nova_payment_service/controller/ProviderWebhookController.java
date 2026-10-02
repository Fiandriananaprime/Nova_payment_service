package org.example.nova_payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.MvolaCallbackRequest;
import org.example.nova_payment_service.dto.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderPaymentRegistrationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.example.nova_payment_service.service.ProviderWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProviderWebhookController {
    private final ProviderWebhookService service;

    @PutMapping("/webhooks/mvola/{callbackToken}")
    public ProviderWebhookResponse receiveMvolaCallback(
            @PathVariable String callbackToken,
            @Valid @RequestBody MvolaCallbackRequest request
    ) {
        return service.receiveMvolaCallback(callbackToken, request);
    }

    @PostMapping("/webhooks/orange-money")
    public ProviderWebhookResponse receiveOrangeNotification(
            @Valid @RequestBody OrangeMoneyNotificationRequest request
    ) {
        return service.receiveOrangeNotification(request);
    }

    @PutMapping("/internal/payments/{transactionId}/provider-payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerProviderPayment(
            @PathVariable UUID transactionId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ProviderPaymentRegistrationRequest request
    ) {
        service.registerProviderPayment(transactionId, authorization, request);
    }
}
