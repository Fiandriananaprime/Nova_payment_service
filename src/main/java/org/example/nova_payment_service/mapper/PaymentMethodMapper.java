package org.example.nova_payment_service.mapper;

import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodMapper {
    public PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod){
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getType(),
                paymentMethod.getProvider(),
                paymentMethod.getProviderPaymentMethodId(),
                paymentMethod.getLabel(),
                paymentMethod.getBrand(),
                paymentMethod.getLast4(),
                paymentMethod.getExpMonth(),
                paymentMethod.getExpYear(),
                paymentMethod.getPhone(),
                paymentMethod.isDefault(),
                paymentMethod.isVerified(),
                paymentMethod.getCreatedAt(),
                paymentMethod.getUpdatedAt()
        );
    }
}
