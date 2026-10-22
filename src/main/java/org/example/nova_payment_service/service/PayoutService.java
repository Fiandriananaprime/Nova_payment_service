package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutDto;
import org.example.nova_payment_service.dto.PayoutStatus;
import org.example.nova_payment_service.entity.Payout;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.example.nova_payment_service.entity.SellerBalanceHistory;
import org.example.nova_payment_service.exception.BalanceNotFound;
import org.example.nova_payment_service.exception.PaymentMethodNotFound;
import org.example.nova_payment_service.repository.PayoutMethodRepository;
import org.example.nova_payment_service.repository.PayoutRepository;
import org.example.nova_payment_service.repository.SellerBalanceHistoryRepository;
import org.example.nova_payment_service.validator.RequestPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final SellerBalanceHistoryRepository historyRepository;
    private final IdempotencyKeyService ikService;
    private final PayoutMethodRepository payoutMethodRepository;
    private final SellerBalanceService sbService;

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

    @Transactional
    public PayoutDto requestPayout(UUID sellerId, UUID ik, RequestPayout requestPayout){
        ikService.getOrCreate(sellerId, ik, requestPayout);

        PayoutMethod payoutMethod = payoutMethodRepository.findByIdAndSellerId(requestPayout.payoutMethodId(),sellerId)
                .orElseThrow(() -> new PaymentMethodNotFound("Payout method not found"));

        Payout payout = Payout.builder()
                .sellerId(sellerId)
                .payoutMethod(payoutMethod)
                .amount(requestPayout.amount())
                .status(PayoutStatus.pending)
                .createdAt(Instant.now())
                .build();

        payoutRepository.save(payout);
        sbService.initiatePayout(requestPayout.amount(),sellerId);
    }
}