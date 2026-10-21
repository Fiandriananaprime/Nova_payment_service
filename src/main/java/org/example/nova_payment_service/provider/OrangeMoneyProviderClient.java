package org.example.nova_payment_service.provider;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


@Component
public class OrangeMoneyProviderClient {

    private final RestClient restClient;
    private final String basicAuthorization;
    private final String merchantKey;
    private final String countrySegment;
    private final String currency;

    private volatile CachedToken cachedToken;

    public OrangeMoneyProviderClient(RestClient.Builder builder, Environment environment) {
        String baseUrl = environment.getProperty("ORANGE_MONEY_BASE_URL", "http://localhost:4010");
        this.restClient = builder.clone().baseUrl(baseUrl).build();
        this.basicAuthorization = Objects.requireNonNull(environment.getProperty(
                "ORANGE_BASIC_AUTH"
        )).trim();
        this.merchantKey = environment.getProperty("ORANGE_MERCHANT_KEY", "local-merchant-key").trim();
        this.countrySegment = environment.getProperty("ORANGE_MONEY_COUNTRY", "dev").trim().toLowerCase();
        this.currency = environment.getProperty("ORANGE_MONEY_CURRENCY", "OUV").trim();
    }

    public WebPaymentResponse createWebPayment(WebPaymentRequest request) {
        validate(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchant_key", merchantKey);
        body.put("currency", currency);
        body.put("order_id", request.orderId().trim());
        body.put("amount", request.amount());
        body.put("return_url", request.returnUrl().trim());
        body.put("cancel_url", request.cancelUrl().trim());
        body.put("notif_url", request.notificationUrl().trim());
        body.put("lang", request.lang() == null || request.lang().isBlank() ? "fr" : request.lang().trim());

        if (request.reference() != null && !request.reference().isBlank()) {
            body.put("reference", request.reference().trim());
        }

        Map<?, ?> response = restClient.post()
                .uri("/orange-money-webpay/{country}/v1/webpayment", countrySegment)
                .headers(headers -> {
                    headers.setBearerAuth(accessToken());
                    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return new WebPaymentResponse(
                intValue(response.get("status"), 201),
                value(response, "message"),
                value(response, "pay_token"),
                value(response, "payment_url"),
                value(response, "notif_token")
        );
    }

    public TransactionStatus getTransactionStatus(String orderId, BigDecimal amount, String payToken) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (payToken == null || payToken.isBlank()) {
            throw new IllegalArgumentException("payToken is required");
        }

        Map<String, Object> body = Map.of(
                "order_id", orderId.trim(),
                "amount", amount,
                "pay_token", payToken.trim()
        );

        Map<?, ?> response = restClient.post()
                .uri("/orange-money-webpay/{country}/v1/transactionstatus", countrySegment)
                .headers(headers -> {
                    headers.setBearerAuth(accessToken());
                    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return new TransactionStatus(
                value(response, "status"),
                value(response, "order_id"),
                value(response, "txnid")
        );
    }

    private synchronized String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.value();
        }

        if (basicAuthorization.isBlank() || !basicAuthorization.startsWith("Basic ")) {
            throw new IllegalStateException("ORANGE_BASIC_AUTH must contain the full Basic authorization header");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        Map<?, ?> response = restClient.post()
                .uri("/oauth/v3/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuthorization)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String token = value(response, "access_token");
        long expiresInSeconds = longValue(response.get("expires_in"), 7_776_000L);
        if (token.isBlank()) {
            throw new IllegalStateException("Orange Money token endpoint returned an empty access_token");
        }

        long cacheSeconds = Math.max(1L, expiresInSeconds - 60L);
        cachedToken = new CachedToken(token, Instant.now().plusSeconds(cacheSeconds));
        return token;
    }

    private void validate(WebPaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (request.returnUrl() == null || request.returnUrl().isBlank()) {
            throw new IllegalArgumentException("returnUrl is required");
        }
        if (request.cancelUrl() == null || request.cancelUrl().isBlank()) {
            throw new IllegalArgumentException("cancelUrl is required");
        }
        if (request.notificationUrl() == null || request.notificationUrl().isBlank()) {
            throw new IllegalArgumentException("notificationUrl is required");
        }
        if (merchantKey.isBlank() || countrySegment.isBlank() || currency.isBlank()) {
            throw new IllegalStateException("Orange Money merchant key, country and currency must be configured");
        }
    }

    private static String value(Map<?, ?> response, String key) {
        if (response == null) {
            throw new IllegalStateException("Orange Money returned an empty response");
        }
        Object value = response.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value, int defaultValue) {
        return (int) longValue(value, defaultValue);
    }

    private static long longValue(Object value, long defaultValue) {
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

    public record WebPaymentRequest(
            String orderId,
            BigDecimal amount,
            String returnUrl,
            String cancelUrl,
            String notificationUrl,
            String lang,
            String reference
    ) {}

    public record WebPaymentResponse(
            int status,
            String message,
            String payToken,
            String paymentUrl,
            String notificationToken
    ) {}

    public record TransactionStatus(String status, String orderId, String transactionId) {}

    private record CachedToken(String value, Instant expiresAt) {}
}
