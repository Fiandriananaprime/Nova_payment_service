package org.example.nova_payment_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPayoutClient {
    private final RestClient restClient;
    private final String apiKey;

    public MockPayoutClient(
            RestClient.Builder builder,
            @Value("${mobile-provider.base-url}") String baseUrl,
            @Value("${mobile-provider.api-key}") String apiKey
    ) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public PayoutProvider.PayoutResult execute(
            String provider,
            String senderMsisdn,
            String recipientMsisdn,
            BigDecimal amount,
            String reference,
            String idempotencyKey
    ) {
        if (senderMsisdn == null || senderMsisdn.isBlank()) {
            throw new IllegalStateException(provider + " mock payout sender MSISDN is not configured");
        }

        MockPayoutResponse response = restClient.post()
                .uri("/internal/payouts")
                .header("X-API-Key", apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MockPayoutRequest(
                        provider,
                        senderMsisdn,
                        recipientMsisdn,
                        amount,
                        reference
                ))
                .retrieve()
                .body(MockPayoutResponse.class);

        if (response == null || response.status() == null || response.status().isBlank()) {
            throw new IllegalStateException("Mock provider returned an empty payout response");
        }

        return new PayoutProvider.PayoutResult(
                response.status(),
                response.id() == null ? reference : response.id().toString(),
                response.failureReason()
        );
    }

    public record MockPayoutRequest(
            String provider,
            String senderMsisdn,
            String recipientMsisdn,
            BigDecimal amount,
            String reference
    ) {}

    public record MockPayoutResponse(
            UUID id,
            String reference,
            String provider,
            String senderMsisdn,
            String recipientMsisdn,
            BigDecimal amount,
            String currency,
            String status,
            String failureReason
    ) {}
}
