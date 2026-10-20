package org.example.nova_payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.entity.SellerBalance;
import org.example.nova_payment_service.service.SellerBalanceService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalPathController {
    private final SellerBalanceService sellerBalanceService;

    @PostMapping("/sellerBalance/{sellerId}")
    public SellerBalance setSellerBalance(
            @RequestHeader("x-user-id") UUID adminId,
            @PathVariable("sellerId") UUID sellerId
    ) {
        return sellerBalanceService.createSellerBalance(sellerId);
    }
}
