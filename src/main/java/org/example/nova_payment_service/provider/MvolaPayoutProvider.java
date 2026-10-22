package org.example.nova_payment_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
    public PayoutResult execute(BigDecimal amount, String recipientMsisdn, String reference, String idempotencyKey) {
        return client.execute("MVOLA", senderMsisdn, recipientMsisdn, amount, reference, idempotencyKey);
    }
}
