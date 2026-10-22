package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutDto;
import org.example.nova_payment_service.entity.Payout;
import org.example.nova_payment_service.entity.SellerBalanceHistory;
import org.example.nova_payment_service.exception.BalanceNotFound;
import org.example.nova_payment_service.repository.PayoutRepository;
import org.example.nova_payment_service.repository.SellerBalanceHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final SellerBalanceHistoryRepository historyRepository;

    private PayoutDto toDTO(Payout payout, BigDecimal newBalance) {
        return new PayoutDto(
                payout.getId(),
                payout.getAmount(),
                payout.getStatus(),
                newBalance,
                payout.getMethod(),
                payout.getCreatedAt(),
                payout.getPaidAt(),
                payout.getFailureReason()
        );
    }

    @Transactional(readOnly = true)
    public Page<PayoutDto> listPayoutsBySellerId(UUID sellerId, Pageable pageable) {
        Page<Payout> payouts = payoutRepository.findBySellerId(sellerId, pageable);

        if (payouts.isEmpty()) {
            return payouts.map(payout -> toDTO(payout, BigDecimal.ZERO));
        }

        List<UUID> payoutIds = payouts.getContent()
                .stream()
                .map(Payout::getId)
                .toList();

        Map<UUID, BigDecimal> balancesByPayoutId = historyRepository.findByPayoutIdIn(payoutIds)
                .stream()
                .collect(Collectors.toMap(
                        SellerBalanceHistory::getPayoutId,
                        SellerBalanceHistory::getBalance
                ));

        return payouts.map(payout -> {
            BigDecimal balance = balancesByPayoutId.get(payout.getId());

            if (balance == null) {
                throw new BalanceNotFound(
                        "Balance history not found for payout " + payout.getId()
                );
            }

            return toDTO(payout, balance);
        });
    }
}