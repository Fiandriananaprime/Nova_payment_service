package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.SellerBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SellerBalanceRepository extends JpaRepository<SellerBalance, UUID> {
    SellerBalance findBySellerId(UUID sellerId);
}
