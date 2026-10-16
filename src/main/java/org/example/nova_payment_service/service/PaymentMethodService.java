
package org.example.nova_payment_service.service;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.CardDetails;
import org.example.nova_payment_service.validator.CreatePaymentMethod;
import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.entity.PaymentMethod;
import org.example.nova_payment_service.entity.PaymentMethodType;
import org.example.nova_payment_service.exception.InvalidCredentials;
import org.example.nova_payment_service.exception.PaymentMethodNotFound;
import org.example.nova_payment_service.mapper.PaymentMethodMapper;
import org.example.nova_payment_service.provider.StripeProvider;
import org.example.nova_payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentRepository repository;
    private final PaymentMethodMapper mapper;
    private final StripeProvider stripeProvider;
    private final PhoneVerificationService phoneService;

    private PaymentMethodResponse createCard(
            UUID userId,
            CreatePaymentMethod method
    ) throws StripeException {

        CardDetails card =
                stripeProvider.getCardDetails(
                        method.providerPaymentMethodId()
                );

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userId(userId)
                .type(PaymentMethodType.card)
                .provider("STRIPE")
                .providerPaymentMethodId(method.providerPaymentMethodId())
                .brand(card.brand())
                .last4(card.last4())
                .expMonth(card.expMonth().shortValue())
                .expYear(card.expYear().shortValue())
                .label(method.label())
                .isDefault(Boolean.TRUE.equals(method.isDefault()))
                .verified(true)
                .build();

        PaymentMethod saved = repository.save(paymentMethod);

        return mapper.toPaymentMethodResponse(saved);
    }

    private PaymentMethodResponse createMobile(
            UUID userId,
            CreatePaymentMethod method
    ) {
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userId(userId)
                .type(method.type())
                .provider(method.provider())
                .phone(method.phone())
                .label(method.label())
                .isDefault(Boolean.TRUE.equals(method.isDefault()))
                .verified(false)
                .build();

        PaymentMethod saved = repository.save(paymentMethod);

        phoneService.requestCode(userId, method.phone());

        return mapper.toPaymentMethodResponse(saved);
    }
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getAll(UUID userId) {
        return repository.findAllByUserId(userId)
                .stream()
                .map(mapper::toPaymentMethodResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse create(
            UUID userId,
            CreatePaymentMethod method
    ) throws StripeException {

        if (method.type() == PaymentMethodType.card) {
            return createCard(userId, method);
        }

        return createMobile(userId, method);
    }

    public void confirmVerification(
            UUID userId,UUID paymentMethodId, String code
    ){
        PaymentMethod pm = repository.findByIdAndUserId(paymentMethodId,userId)
                .orElseThrow(() -> new PaymentMethodNotFound("User dont match with payment method"));

        if (pm.getPhone() == null || pm.getPhone().isBlank()) {
            throw new PaymentMethodNotFound(
                    "Payment method has no phone number"
            );}
        boolean valid = phoneService.verifyCode(userId, pm.getPhone(), code);
        if(!valid) throw new InvalidCredentials("Incorrect code");

        pm.setVerified(true);
        repository.save(pm);

    }
}
