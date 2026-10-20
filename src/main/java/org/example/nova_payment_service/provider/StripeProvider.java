package org.example.nova_payment_service.provider;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.example.nova_payment_service.dto.CardDetails;
import org.example.nova_payment_service.exception.InvalidPaymentMethod;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * Reusable Stripe client.
 *
 * This client deliberately accepts only Stripe test-mode secret keys. Amounts passed
 * to PaymentIntent methods must already be in the smallest unit expected by Stripe.
 */
@Component
public class StripeProvider {

    private final String secretKey;

    public StripeProvider(Environment environment) {
        this.secretKey = environment.getProperty("STRIPE_SECRET_KEY", "").trim();
    }

    public CardDetails getCardDetails(String paymentMethodId) throws StripeException {
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new IllegalArgumentException("paymentMethodId is required");
        }

        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId, requestOptions(null));

        if (!"card".equals(paymentMethod.getType()) || paymentMethod.getCard() == null) {
            throw new InvalidPaymentMethod("Invalid Card");
        }

        PaymentMethod.Card card = paymentMethod.getCard();
        return new CardDetails(
                paymentMethodId,
                card.getBrand(),
                card.getLast4(),
                card.getExpMonth(),
                card.getExpYear()
        );
    }

    /**
     * Creates and confirms a card PaymentIntent.
     *
     * The idempotency key is forwarded to Stripe so retries of the same operation
     * can reuse the same provider result. A new logical payment needs a new key.
     */
    public PaymentIntent createPaymentIntent(
            long amountMinorUnits,
            String currency,
            String paymentMethodId,
            String description,
            String idempotencyKey
    ) throws StripeException {
        if (amountMinorUnits <= 0) {
            throw new IllegalArgumentException("amountMinorUnits must be greater than zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new IllegalArgumentException("paymentMethodId is required");
        }

        PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinorUnits)
                .setCurrency(currency.trim().toLowerCase(Locale.ROOT))
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true);

        if (description != null && !description.isBlank()) {
            params.setDescription(description.trim());
        }

        return PaymentIntent.create(params.build(), requestOptions(idempotencyKey));
    }

    public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId is required");
        }
        return PaymentIntent.retrieve(paymentIntentId, requestOptions(null));
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        if (secretKey.isBlank() || !secretKey.startsWith("sk_test_")) {
            throw new IllegalStateException(
                    "Configure STRIPE_SECRET_KEY with a Stripe test-mode secret key (sk_test_...)."
            );
        }

        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(secretKey);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey.trim());
        }
        return builder.build();
    }
}
