package org.example.nova_payment_service.service;

import org.example.nova_payment_service.dto.MvolaCallbackRequest;
import org.example.nova_payment_service.dto.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderPaymentRegistrationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProviderWebhookService {
    private final JdbcTemplate jdbcTemplate;
    private final String mvolaCallbackToken;
    private final String internalApiToken;

    public ProviderWebhookService(
            JdbcTemplate jdbcTemplate,
            @Value("${payments.webhooks.mvola-token:}") String mvolaCallbackToken,
            @Value("${payments.internal-api-token:}") String internalApiToken
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mvolaCallbackToken = mvolaCallbackToken;
        this.internalApiToken = internalApiToken;
    }

    @Transactional
    public void registerProviderPayment(
            UUID transactionId,
            String authorization,
            ProviderPaymentRegistrationRequest request
    ) {
        requireInternalAuthorization(authorization);

        String provider = request.provider();
        String method = switch (provider) {
            case "MVOLA" -> "mvola";
            case "ORANGE_MONEY" -> "orange_money";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment provider");
        };
        String notificationTokenHash = null;
        if ("ORANGE_MONEY".equals(provider)) {
            if (request.notificationToken() == null || request.notificationToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orange Money notificationToken is required");
            }
            notificationTokenHash = sha256(request.notificationToken());
        } else if (request.notificationToken() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MVola does not use an Orange notification token");
        }

        try {
            int updated = jdbcTemplate.update("""
                    UPDATE transactions
                    SET provider_reference = ?,
                        provider_notification_token_hash = ?
                    WHERE id = ?
                      AND method = ?::transaction_method
                      AND status = 'pending'
                      AND (provider_reference IS NULL OR provider_reference = ?)
                      AND (provider_notification_token_hash IS NULL
                           OR provider_notification_token_hash = ?)
                    """,
                    request.providerReference(),
                    notificationTokenHash,
                    transactionId,
                    method,
                    request.providerReference(),
                    notificationTokenHash
            );
            if (updated == 1) return;
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider payment reference is already registered");
        }

        TransactionState state = findTransaction(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction not found"));
        if (!method.equals(state.method()) || !"pending".equals(state.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a pending transaction for this provider can be linked");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already has a different provider reference");
    }

    public ProviderWebhookResponse receiveMvolaCallback(String callbackToken, MvolaCallbackRequest request) {
        requireConfiguredSecret(mvolaCallbackToken, callbackToken, "MVola callback authentication is not configured");
        String targetStatus = request.transactionStatus();
        int updated = jdbcTemplate.update("""
                UPDATE transactions
                SET status = ?::transaction_status,
                    provider_transaction_reference = ?,
                    updated_at = NOW()
                WHERE method = 'mvola'
                  AND provider_reference = ?
                  AND status = 'pending'
                """, targetStatus, request.transactionReference(), request.serverCorrelationId());
        if (updated == 1) return new ProviderWebhookResponse(targetStatus, false);

        TransactionState state = findByProviderReference("mvola", request.serverCorrelationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MVola transaction not found"));
        if (targetStatus.equals(state.status())) return new ProviderWebhookResponse(state.status(), true);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already has a different final status");
    }

    public ProviderWebhookResponse receiveOrangeNotification(OrangeMoneyNotificationRequest request) {
        String targetStatus = switch (request.status()) {
            case "SUCCESS" -> "completed";
            case "FAILED", "EXPIRED" -> "failed";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Orange Money status");
        };
        String tokenHash = sha256(request.notif_token());
        int updated = jdbcTemplate.update("""
                UPDATE transactions
                SET status = ?::transaction_status,
                    provider_transaction_reference = COALESCE(?, provider_transaction_reference),
                    updated_at = NOW()
                WHERE method = 'orange_money'
                  AND provider_notification_token_hash = ?
                  AND status = 'pending'
                """, targetStatus, request.txnid(), tokenHash);
        if (updated == 1) return new ProviderWebhookResponse(targetStatus, false);

        TransactionState state = findByOrangeNotificationToken(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orange Money notification token not found"));
        if (targetStatus.equals(state.status())) return new ProviderWebhookResponse(state.status(), true);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already has a different final status");
    }

    private void requireInternalAuthorization(String authorization) {
        String expected = configuredSecret(internalApiToken, "Internal payment API authentication is not configured");
        String prefix = "Bearer ";
        if (authorization == null || !authorization.startsWith(prefix) ||
                !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        authorization.substring(prefix.length()).getBytes(StandardCharsets.UTF_8)
                )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal payment API credentials");
        }
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

    private Optional<TransactionState> findTransaction(UUID transactionId) {
        return jdbcTemplate.query("""
                        SELECT method::text, status::text
                        FROM transactions
                        WHERE id = ?
                        """,
                (resultSet, rowNumber) -> new TransactionState(resultSet.getString(1), resultSet.getString(2)),
                transactionId
        ).stream().findFirst();
    }

    private Optional<TransactionState> findByProviderReference(String method, String reference) {
        return jdbcTemplate.query("""
                        SELECT method::text, status::text
                        FROM transactions
                        WHERE method = ?::transaction_method
                          AND provider_reference = ?
                        """,
                (resultSet, rowNumber) -> new TransactionState(resultSet.getString(1), resultSet.getString(2)),
                method,
                reference
        ).stream().findFirst();
    }

    private Optional<TransactionState> findByOrangeNotificationToken(String tokenHash) {
        return jdbcTemplate.query("""
                        SELECT method::text, status::text
                        FROM transactions
                        WHERE method = 'orange_money'
                          AND provider_notification_token_hash = ?
                        """,
                (resultSet, rowNumber) -> new TransactionState(resultSet.getString(1), resultSet.getString(2)),
                tokenHash
        ).stream().findFirst();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record TransactionState(String method, String status) {
    }
}
