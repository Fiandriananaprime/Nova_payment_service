package org.example.nova_payment_service.provider;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import org.example.nova_payment_service.dto.CardDetails;
import org.example.nova_payment_service.exception.InvalidPaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class StripeProvider {

    public CardDetails getCardDetails(String paymentMethodId) throws StripeException {
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);

        if(!"card".equals(pm.getType()) || pm.getCard() == null) throw new InvalidPaymentMethod("Invalid Card");

        PaymentMethod.Card card = pm.getCard();

        return new CardDetails(paymentMethodId,card.getBrand(),card.getLast4(),card.getExpMonth(),card.getExpYear());
    }
}
