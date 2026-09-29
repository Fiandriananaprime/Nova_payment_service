package org.example.nova_payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.service.PaymentMethodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/buyer/payment-methods")
public class PaymentMethodController {
    private PaymentMethodService service;

    @GetMapping
    public List<PaymentMethodResponse> getAll (
            @RequestAttribute UUID userId
    ){
        return service.getAll(userId);
    }
}
