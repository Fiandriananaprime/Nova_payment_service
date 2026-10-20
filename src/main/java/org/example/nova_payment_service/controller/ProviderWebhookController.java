package org.example.nova_payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.validator.MvolaCallbackRequest;
import org.example.nova_payment_service.validator.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.example.nova_payment_service.service.ProviderWebhookService;
import org.springframework.web.bind.annotation.*;

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
}
