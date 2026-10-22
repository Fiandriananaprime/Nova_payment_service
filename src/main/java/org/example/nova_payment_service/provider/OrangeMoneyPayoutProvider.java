package org.example.nova_payment_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrangeMoneyPayoutProvider implements PayoutProvider {
    private final MockPayoutClient client;
    private final String senderMsisdn;

    public OrangeMoneyPayoutProvider(
            MockPayoutClient client,
            @Value("${mobile-provider.orange-sender-msisdn:}") String senderMsisdn
    ) {
        this.client = client;
        this.senderMsisdn = senderMsisdn;
    }

    @Override
    public PayoutResult execute(BigDecimal amount, String recipientMsisdn, String reference, String idempotencyKey) {
        return client.execute("ORANGE_MONEY", senderMsisdn, recipientMsisdn, amount, reference, idempotencyKey);
    }
}
