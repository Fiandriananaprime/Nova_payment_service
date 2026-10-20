package org.example.nova_payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.validator.CreatePayoutMethod;
import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.service.PayoutMethodService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seller/payout-methods")
@RequiredArgsConstructor
public class PayoutMethodController {
    private final PayoutMethodService service;

    @GetMapping
    public List<PayoutMethodDto> findAll(
            @RequestHeader("x-user-id") UUID sellerId
    ){
        return service.findPayoutMethodBySellerId(sellerId);
    }

    @PostMapping
    public PayoutMethodDto create(
            @RequestHeader("x-user-id") UUID sellerId,
            @Valid @RequestBody CreatePayoutMethod body
            ){
        return service.createPayoutMethod(sellerId,body);
    }

}
