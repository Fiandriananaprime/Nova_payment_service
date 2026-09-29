package org.example.nova_payment_service.service;


import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PaymentMethodResponse;
import org.example.nova_payment_service.entity.PaymentMethod;
import org.example.nova_payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentMethodService {
    private final PaymentRepository repository;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getAll (UUID userId){
        return repository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod){
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getType(),
                paymentMethod.getProvider(),
                paymentMethod.getLabel(),
                paymentMethod.getBrand(),
                paymentMethod.getLast4(),
                paymentMethod.getExpMonth(),
                paymentMethod.getExpYear(),
                paymentMethod.getPhone(),
                paymentMethod.isDefault(),
                paymentMethod.isVerified(),
                paymentMethod.getCreatedAt(),
                paymentMethod.getUpdatedAt()
        );
    }
}
