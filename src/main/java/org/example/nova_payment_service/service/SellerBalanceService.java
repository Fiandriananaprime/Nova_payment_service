package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.entity.SellerBalance;
import org.example.nova_payment_service.entity.SellerBalanceHistory;
import org.example.nova_payment_service.exception.BalanceException;
import org.example.nova_payment_service.exception.BalanceNotFound;

import org.example.nova_payment_service.repository.SellerBalanceHistoryRepository;
import org.example.nova_payment_service.repository.SellerBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerBalanceService {
    private final SellerBalanceRepository repository;
    private final SellerBalanceHistoryRepository historyRepository;

    public SellerBalance createSellerBalance(UUID sellerId) {
        return repository.findBySellerId(sellerId)
                .orElseGet(() -> repository.save(new SellerBalance(sellerId)));
    }

    @Transactional
    public SellerBalance initiatePayout(BigDecimal amount, UUID sellerId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BalanceException("Amount must be positive");
        }

        SellerBalance sb = repository.findBySellerId(sellerId)
                .orElseThrow(() -> new BalanceNotFound("User " + sellerId + " not found"));

        if (sb.getAvailableAmount().compareTo(amount) < 0) {
            throw new BalanceException("Balance not enough");
        }

        sb.setAvailableAmount(sb.getAvailableAmount().subtract(amount));
        sb.setReservedAmount(sb.getReservedAmount().add(amount));

        return repository.save(sb);
    }

    @Transactional
    public SellerBalance confirmPayout(UUID sellerId, UUID payoutId, BigDecimal amount) {
        SellerBalance sb = repository.findBySellerId(sellerId)
                .orElseThrow(() -> new BalanceNotFound("User " + sellerId + " not found"));

        if (sb.getReservedAmount().compareTo(amount) < 0) {
            throw new BalanceException("Reserved balance not enough");
        }

        sb.setReservedAmount(sb.getReservedAmount().subtract(amount));

        historyRepository.save(SellerBalanceHistory.builder()
                .sellerId(sellerId)
                .balance(sb.getAvailableAmount())
                .currency("MGA")
                .payoutId(payoutId)
                .recordedAt(Instant.now())
                .build());

        return repository.save(sb);
    }
}
