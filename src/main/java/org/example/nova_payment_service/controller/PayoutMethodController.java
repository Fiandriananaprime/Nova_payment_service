package org.example.nova_payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.validator.CreatePayoutMethod;
import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.service.PayoutMethodService;
import org.example.nova_payment_service.validator.PayoutUpdate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/payout-methods")
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

    @PatchMapping("/{payoutMethodId}")
    public PayoutMethodDto  update(
            @PathVariable("payoutMethodId") UUID pmId,
            @Valid @RequestBody PayoutUpdate request,
            @RequestHeader("x-user-id") UUID userId
            ){
        return service.updatePayoutMethod(userId,pmId,request);
    }
}
