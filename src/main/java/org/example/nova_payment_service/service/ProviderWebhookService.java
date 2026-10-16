package org.example.nova_payment_service.service;

import org.example.nova_payment_service.dto.MvolaCallbackRequest;
import org.example.nova_payment_service.dto.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.example.nova_payment_service.repository.ProviderWebhookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ProviderWebhookService {
    private final ProviderWebhookRepository repository;
    private final String mvolaCallbackToken;

    public ProviderWebhookService(
            ProviderWebhookRepository repository,
            @Value("${payments.webhooks.mvola-token:}") String mvolaCallbackToken
    ) {
        this.repository = repository;
        this.mvolaCallbackToken = mvolaCallbackToken;
    }

    public ProviderWebhookResponse receiveMvolaCallback(String callbackToken, MvolaCallbackRequest request) {
        requireConfiguredSecret(mvolaCallbackToken, callbackToken, "MVola callback authentication is not configured");
        String targetStatus = request.transactionStatus();
        int updated = repository.completeMvolaPayment(
                targetStatus,
                request.transactionReference(),
                request.serverCorrelationId()
        );
        if (updated == 1) return new ProviderWebhookResponse(targetStatus, false);

        ProviderWebhookRepository.ProviderPaymentState state = repository.findStateByProviderReference(
                        "mvola", request.serverCorrelationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MVola transaction not found"));
        if (targetStatus.equals(state.getStatus())) return new ProviderWebhookResponse(state.getStatus(), true);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already has a different final status");
    }

    public ProviderWebhookResponse receiveOrangeNotification(OrangeMoneyNotificationRequest request) {
        String targetStatus = switch (request.status()) {
            case "SUCCESS" -> "completed";
            case "FAILED", "EXPIRED" -> "failed";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Orange Money status");
        };
        String tokenHash = sha256(request.notif_token());
        int updated = repository.completeOrangeMoneyPayment(targetStatus, request.txnid(), tokenHash);
        if (updated == 1) return new ProviderWebhookResponse(targetStatus, false);

        ProviderWebhookRepository.ProviderPaymentState state = repository.findStateByNotificationTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orange Money notification token not found"));
        if (targetStatus.equals(state.getStatus())) return new ProviderWebhookResponse(state.getStatus(), true);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already has a different final status");
    }

    private void requireConfiguredSecret(String expected, String provided, String notConfiguredMessage) {
        expected = configuredSecret(expected, notConfiguredMessage);
        if (provided == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid provider callback credentials");
        }
    }

    private static String configuredSecret(String value, String message) {
        if (value == null || value.length() < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
