package org.example.nova_payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.service.PayoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/internal/webhooks/mock-payout")
@RequiredArgsConstructor
public class MockPayoutWebhookController {
    private final PayoutService payoutService;

    @Value("${mobile-provider.api-key:}")
    private String expectedApiKey;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody PayoutCallback callback
    ) {
        if (expectedApiKey == null || expectedApiKey.isBlank() || apiKey == null ||
                !MessageDigest.isEqual(
                        expectedApiKey.getBytes(StandardCharsets.UTF_8),
                        apiKey.getBytes(StandardCharsets.UTF_8)
                )) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (callback.payoutId() == null || callback.provider() == null ||
                callback.status() == null || callback.amount() == null ||
                callback.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        payoutService.handleMockPayoutCallback(
                callback.payoutId(),
                callback.provider(),
                callback.status(),
                callback.amount(),
                callback.failureReason()
        );
        return ResponseEntity.noContent().build();
    }

    public record PayoutCallback(
            UUID payoutId,
            String reference,
            String provider,
            String status,
            String failureReason,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {}
}
