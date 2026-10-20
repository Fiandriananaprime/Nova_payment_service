package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutMethodRepository extends JpaRepository<PayoutMethod, UUID> {

    List<PayoutMethod> findAllBySellerId(UUID sellerId);

    Optional<PayoutMethod> findByIdAndSellerId(UUID id, UUID sellerId);
}
