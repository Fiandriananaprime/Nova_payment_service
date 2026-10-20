package org.example.nova_payment_service.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.validator.CreatePaymentMethod;
import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.validator.VerifyPhoneRequest;
import org.example.nova_payment_service.validator.UpdatePaymentMethod;
import org.example.nova_payment_service.service.PaymentMethodService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/buyer/payment-methods")
public class PaymentMethodController {
    private final PaymentMethodService service;

    @GetMapping
    public List<PaymentMethodResponse> getAll (
            @RequestHeader("x-user-id") UUID userId
    ){
        return service.getAll(userId);
    }

    @PostMapping
    public PaymentMethodResponse createPaymentMethod (
            @RequestHeader("x-user-id") UUID userId,
            @Valid @RequestBody CreatePaymentMethod request
    ) throws StripeException {
        return service.create(userId,request);
    }

    @PatchMapping("/{paymentMethodId}")
    public PaymentMethodResponse updatePaymentMethod(
            @RequestHeader("x-user-id") UUID userId,
            @PathVariable UUID paymentMethodId,
            @Valid @RequestBody UpdatePaymentMethod request
    ) {
        return service.update(userId, paymentMethodId, request);
    }

    @PostMapping("/{paymentMethodId}/confirm")
    public void confirmVerification(
            @RequestHeader("x-user-id") UUID userId,
            @PathVariable UUID paymentMethodId,
            @Valid @RequestBody VerifyPhoneRequest request
    ) {
        service.confirmVerification(userId, paymentMethodId, request.code());
    }

    @DeleteMapping("/{paymentMethodId}")
    public void DeletePaymentMethod(
            @RequestHeader("x-user-id") UUID userId,
            @PathVariable UUID paymentMethodId
    ){
        service.deletePaymentMethod(userId,paymentMethodId);
    }
}
