package org.example.nova_payment_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SmsService {
    private final RestClient restClient;
    private final String webhookUrl;
    private final String apiKey;

    public SmsService(
            RestClient.Builder builder,
            @Value("${sms.webhook-url}") String webhookUrl,
            @Value(("${sms.api-key}")) String apiKey

    ) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
        this.apiKey = apiKey;
    }

    public void send(String target, String message) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key",apiKey)
                .body(new SmsRequest(target, message))
                .retrieve()
                .toBodilessEntity();
    }

    private record SmsRequest(String phoneNumber, String message) {}
}
