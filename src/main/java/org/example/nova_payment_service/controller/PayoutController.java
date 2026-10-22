package org.example.nova_payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutDto;
import org.example.nova_payment_service.service.PayoutService;
import org.example.nova_payment_service.validator.RequestPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/payouts")
public class PayoutController {
    private final PayoutService payoutService;

    @GetMapping
    public Page<PayoutDto> getPayoutsHistory(
            @RequestHeader("x-user-id") UUID sellerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name= "limit", defaultValue = "20") int limit
    ){
        return payoutService.listPayoutsBySellerId(sellerId, PageRequest.of(page,limit));
    }

    @PostMapping
    public PayoutDto requestPayout(
            @RequestHeader("x-user-id") UUID sellerId,
            @RequestHeader("Idempotency-key") UUID idempotencyKey,
            @Valid @RequestBody RequestPayout requestPayout
            ){
        return payoutService.requestPayout(sellerId, idempotencyKey,requestPayout);
    }
}
