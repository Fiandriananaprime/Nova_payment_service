package org.example.nova_payment_service.provider;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable MVola Merchant Pay client for the provider HTTP contract used by
 * MockMobileProvider. Configure MVOLA_BASE_URL to point at the MVola sandbox
 * when not using the local simulator.
 *
 * Note: Merchant Pay here is a customer-to-merchant collection flow. It is not
 * a merchant disbursement/payout API.
 */
@Component
public class MvolaProviderClient {

    private static final String TRANSACTION_BASE_PATH =
            "/mvola/mm/transactions/type/merchantpay/1.0.0/";

    private final RestClient restClient;
    private final String clientKey;
    private final String clientSecret;
    private final String merchantMsisdn;
    private final String partnerName;
    private final String userLanguage;

    private volatile CachedToken cachedToken;

    public MvolaProviderClient(RestClient.Builder builder, Environment environment) {
        String baseUrl = environment.getProperty("MVOLA_BASE_URL", "http://localhost:4010");
        this.restClient = builder.clone().baseUrl(baseUrl).build();
        this.clientKey = environment.getProperty("MVOLA_CLIENT_KEY", "local-test-key").trim();
        this.clientSecret = environment.getProperty("MVOLA_CLIENT_SECRET", "local-test-secret").trim();
        this.merchantMsisdn = environment.getProperty("MVOLA_MERCHANT_MSISDN", "0343500001").trim();
        this.partnerName = environment.getProperty("MVOLA_PARTNER_NAME", "NovaMarket").trim();
        this.userLanguage = environment.getProperty("MVOLA_USER_LANGUAGE", "FR").trim().toUpperCase();
    }

    /**
     * Initiates a customer-to-merchant MVola payment and returns the provider's
     * pending reference. The transaction must be polled or reconciled by callback.
     */
    public InitiationResponse initiatePayment(PaymentRequest request) {
        validatePaymentRequest(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", request.amount().stripTrailingZeros().toPlainString());
        body.put("currency", "Ar");
        body.put("descriptionText", request.description().trim());
        body.put("requestingOrganisationTransactionReference", request.transactionReference().trim());
        body.put("originalTransactionReference", request.transactionReference().trim());
        body.put("requestDate", Instant.now().toString());
        body.put("debitParty", List.of(party(request.customerMsisdn().trim())));
        body.put("creditParty", List.of(party(merchantMsisdn)));

        List<KeyValue> metadata = new ArrayList<>();
        metadata.add(new KeyValue("partnerName", partnerName));
        if (request.metadata() != null) {
            metadata.addAll(request.metadata());
        }
        body.put("metadata", metadata);

        Map<?, ?> response = restClient.post()
                .uri(TRANSACTION_BASE_PATH)
                .headers(headers -> {
                    setCommonHeaders(headers);
                    if (request.callbackUrl() != null && !request.callbackUrl().isBlank()) {
                        headers.set("X-Callback-URL", request.callbackUrl().trim());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return new InitiationResponse(
                value(response, "status"),
                value(response, "serverCorrelationId"),
                value(response, "notificationMethod")
        );
    }

    public TransactionStatus getTransactionStatus(String serverCorrelationId) {
        if (serverCorrelationId == null || serverCorrelationId.isBlank()) {
            throw new IllegalArgumentException("serverCorrelationId is required");
        }

        Map<?, ?> response = restClient.get()
                .uri(TRANSACTION_BASE_PATH + "status/{serverCorrelationId}", serverCorrelationId.trim())
                .headers(this::setCommonHeaders)
                .retrieve()
                .body(Map.class);

        return new TransactionStatus(
                value(response, "status"),
                value(response, "serverCorrelationId"),
                value(response, "notificationMethod"),
                value(response, "objectReference")
        );
    }

    public Map<?, ?> getTransactionDetails(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException("transactionReference is required");
        }

        return restClient.get()
                .uri(TRANSACTION_BASE_PATH + "{transactionReference}", transactionReference.trim())
                .headers(this::setCommonHeaders)
                .retrieve()
                .body(Map.class);
    }

    private synchronized String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.value();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "EXT_INT_MVOLA_SCOPE");

        String credentials = clientKey + ":" + clientSecret;
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<?, ?> response = restClient.post()
                .uri("/token")
                .header(HttpHeaders.AUTHORIZATION, basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String token = value(response, "access_token");
        long expiresInSeconds = numberValue(response.get("expires_in"), 3600L);
        if (token.isBlank()) {
            throw new IllegalStateException("MVola token endpoint returned an empty access_token");
        }

        long cacheSeconds = Math.max(1L, expiresInSeconds - 60L);
        cachedToken = new CachedToken(token, Instant.now().plusSeconds(cacheSeconds));
        return token;
    }

    private void setCommonHeaders(HttpHeaders headers) {
        if (!List.of("FR", "MG").contains(userLanguage)) {
            throw new IllegalStateException("MVOLA_USER_LANGUAGE must be FR or MG");
        }
        headers.setBearerAuth(accessToken());
        headers.set("Version", "1.0");
        headers.set("X-CorrelationID", UUID.randomUUID().toString());
        headers.set("UserLanguage", userLanguage);
        headers.set("UserAccountIdentifier", "msisdn;" + merchantMsisdn);
        headers.set("partnerName", partnerName);
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (request.transactionReference() == null || request.transactionReference().isBlank()) {
            throw new IllegalArgumentException("transactionReference is required");
        }
        if (request.customerMsisdn() == null || request.customerMsisdn().isBlank()) {
            throw new IllegalArgumentException("customerMsisdn is required");
        }
        if (merchantMsisdn.isBlank() || clientKey.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("MVola merchant number and client credentials must be configured");
        }
    }

    private static Map<String, String> party(String msisdn) {
        return Map.of("key", "msisdn", "value", msisdn);
    }

    private static String value(Map<?, ?> response, String key) {
        if (response == null) {
            throw new IllegalStateException("MVola returned an empty response");
        }
        Object value = response.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long numberValue(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public record PaymentRequest(
            BigDecimal amount,
            String description,
            String transactionReference,
            String customerMsisdn,
            String callbackUrl,
            List<KeyValue> metadata
    ) {}

    public record KeyValue(String key, String value) {}

    public record InitiationResponse(String status, String serverCorrelationId, String notificationMethod) {}

    public record TransactionStatus(
            String status,
            String serverCorrelationId,
            String notificationMethod,
            String objectReference
    ) {}

    private record CachedToken(String value, Instant expiresAt) {}
}
