package org.example.nova_payment_service.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.CreatePaymentMethod;
import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.service.PaymentMethodService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/buyer/payment-methods")
public class PaymentMethodController {
    private PaymentMethodService service;

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
}
