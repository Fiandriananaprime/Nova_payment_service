package org.example.nova_payment_service.provider;

import org.example.nova_payment_service.dto.ProviderAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MobileProviderClient {

    private final RestClient restClient;
    private final String apiKey;

    public MobileProviderClient(
            RestClient.Builder builder,
            @Value("${mobile-provider.base-url}") String baseUrl,
            @Value("${mobile-provider.api-key}") String apiKey
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    public ProviderAccount findAccount(String provider, String msisdn) {
        return restClient.get()
                .uri("/internal/accounts/{provider}/{msisdn}", provider, msisdn)
                .header("X-API-Key", apiKey)
                .retrieve()
                .body(ProviderAccount.class);
    }
}