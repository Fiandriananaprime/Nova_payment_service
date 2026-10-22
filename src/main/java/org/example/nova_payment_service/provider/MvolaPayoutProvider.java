package org.example.nova_payment_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MvolaPayoutProvider implements PayoutProvider {
    private final MockPayoutClient client;
    private final String senderMsisdn;

    public MvolaPayoutProvider(
            MockPayoutClient client,
            @Value("${mobile-provider.mvola-sender-msisdn:}") String senderMsisdn
    ) {
        this.client = client;
        this.senderMsisdn = senderMsisdn;
    }

    @Override
    public void validateConfiguration() {
        client.validateConfiguration();
        if (senderMsisdn == null || senderMsisdn.isBlank()) {
            throw new IllegalStateException("MVOLA_MERCHANT_MSISDN must identify an existing mock MVola wallet");
        }
    }

    @Override
    public PayoutResult execute(BigDecimal amount, String recipientMsisdn, String reference, String idempotencyKey, UUID payoutId) {
        return client.execute("MVOLA", senderMsisdn, recipientMsisdn, amount, reference, idempotencyKey, payoutId);
    }
}
