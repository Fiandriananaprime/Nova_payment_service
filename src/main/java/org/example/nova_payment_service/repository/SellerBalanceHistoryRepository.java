package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.SellerBalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SellerBalanceHistoryRepository extends JpaRepository<SellerBalanceHistory, UUID> {
    List<SellerBalanceHistory> findByPayoutIdIn(Collection<UUID> payoutIds);
}
