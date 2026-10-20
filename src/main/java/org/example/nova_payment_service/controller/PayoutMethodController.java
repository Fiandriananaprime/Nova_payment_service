package org.example.nova_payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.service.PayoutMethodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
