package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.entity.SellerBalance;
import org.example.nova_payment_service.exception.ConflictException;
import org.example.nova_payment_service.repository.SellerBalanceRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerBalanceService {
    private final SellerBalanceRepository repository;

    public SellerBalance createSellerBalance(UUID sellerId){
        SellerBalance sb = repository.findBySellerId(sellerId);
        if(sb != null) throw new ConflictException("Seller Balance already set");
        return repository.save(new SellerBalance(sellerId));
    }
}
