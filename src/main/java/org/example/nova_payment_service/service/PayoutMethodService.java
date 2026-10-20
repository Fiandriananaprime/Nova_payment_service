package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.example.nova_payment_service.repository.PayoutMethodRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutMethodService {
    private final PayoutMethodRepository repository;

    public List<PayoutMethodDto> findPayoutMethodBySellerId(UUID sellerId){
        List<PayoutMethod> payoutMethods = repository.findAllBySellerId(sellerId);

        return payoutMethods.stream().map(
                (p) -> new PayoutMethodDto(
                        p.getId(),
                        p.getType(),
                        p.getLabel(),
                        p.getAccountName(),
                        p.getMaskedAccount(),
                        p.isDefault(),
                        p.isVerified())
        ).toList();
    }
}
