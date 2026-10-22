package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.IdempotencyKeyStatus;
import org.example.nova_payment_service.dto.PayoutDto;
import org.example.nova_payment_service.dto.PayoutMethodType;
import org.example.nova_payment_service.dto.PayoutStatus;
import org.example.nova_payment_service.entity.IdempotencyKey;
import org.example.nova_payment_service.entity.Payout;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.example.nova_payment_service.entity.SellerBalance;
import org.example.nova_payment_service.entity.SellerBalanceHistory;
import org.example.nova_payment_service.exception.BalanceNotFound;
import org.example.nova_payment_service.exception.PaymentMethodNotFound;
import org.example.nova_payment_service.exception.PaymentMethodNotHandled;
import org.example.nova_payment_service.provider.MvolaPayoutProvider;
import org.example.nova_payment_service.provider.OrangeMoneyPayoutProvider;
import org.example.nova_payment_service.provider.PayoutProvider;
import org.example.nova_payment_service.repository.PayoutMethodRepository;
import org.example.nova_payment_service.repository.PayoutRepository;
import org.example.nova_payment_service.repository.SellerBalanceHistoryRepository;
import org.example.nova_payment_service.service.AccountEncryptionService;
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
    private final AccountEncryptionService encryptionService;
    private final MvolaPayoutProvider mvolaPayoutProvider;
    private final OrangeMoneyPayoutProvider orangeMoneyPayoutProvider;

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

        List<UUID> payoutIds = payouts.getContent().stream().map(Payout::getId).toList();
        Map<UUID, BigDecimal> balancesByPayoutId = historyRepository.findByPayoutIdIn(payoutIds)
                .stream()
                .collect(Collectors.toMap(
                        SellerBalanceHistory::getPayoutId,
                        SellerBalanceHistory::getBalance,
                        (first, ignored) -> first
                ));

        return payouts.map(payout -> {
            BigDecimal balance = balancesByPayoutId.get(payout.getId());
            if (balance == null) {
                throw new BalanceNotFound("Balance history not found for payout " + payout.getId());
            }
            return toDTO(payout, balance);
        });
    }

    @Transactional
    public PayoutDto requestPayout(UUID sellerId, UUID ik, RequestPayout requestPayout) {
        IdempotencyKey idempotency = ikService.getOrCreate(sellerId, ik, requestPayout);

        if (idempotency.getPayoutId() != null) {
            Payout previous = payoutRepository.findById(idempotency.getPayoutId())
                    .orElseThrow(() -> new IllegalStateException("Idempotent payout record was not found"));
            BigDecimal previousBalance = historyRepository.findByPayoutIdIn(List.of(previous.getId()))
                    .stream()
                    .findFirst()
                    .map(SellerBalanceHistory::getBalance)
                    .orElse(BigDecimal.ZERO);
            return toDTO(previous, previousBalance);
        }

        PayoutMethod payoutMethod = payoutMethodRepository
                .findByIdAndSellerId(requestPayout.payoutMethodId(), sellerId)
                .orElseThrow(() -> new PaymentMethodNotFound("Payout method not found"));

        if (!payoutMethod.isVerified()) {
            throw new IllegalStateException("Payout method must be verified before requesting a payout");
        }

        PayoutProvider provider = switch (payoutMethod.getType()) {
            case mvola -> mvolaPayoutProvider;
            case orange_money -> orangeMoneyPayoutProvider;
            case bank_account -> throw new PaymentMethodNotHandled(
                    "Bank account payouts are not supported by the configured mock providers"
            );
        };

        provider.validateConfiguration();
        String recipientMsisdn = encryptionService.decrypt(payoutMethod.getAccountEncrypted());
        String providerIdempotencyKey = sellerId + ":" + ik;
        String reference = "nova-payout-" + sellerId + "-" + ik;

        SellerBalance reservedBalance = sbService.initiatePayout(requestPayout.amount(), sellerId);
        Payout payout = Payout.builder()
                .sellerId(sellerId)
                .payoutMethod(payoutMethod)
                .method(payoutMethod.getType())
                .amount(requestPayout.amount())
                .status(PayoutStatus.processing)
                .createdAt(Instant.now())
                .build();
        payoutRepository.save(payout);
        idempotency.setPayoutId(payout.getId());

        try {
            PayoutProvider.PayoutResult result = provider.execute(
                    requestPayout.amount(),
                    recipientMsisdn,
                    reference,
                    providerIdempotencyKey
            );

            if (result.completed()) {
                SellerBalance settledBalance = sbService.confirmPayout(
                        sellerId, payout.getId(), requestPayout.amount()
                );
                payout.setStatus(PayoutStatus.completed);
                payout.setPaidAt(Instant.now());
                payout.setFailureReason(null);
                idempotency.setStatus(IdempotencyKeyStatus.COMPLETED);
                idempotency.setResponseStatus(200);
                idempotency.setResponseBody(Map.of(
                        "payoutId", payout.getId().toString(),
                        "status", payout.getStatus().name()
                ));
                return toDTO(payoutRepository.save(payout), settledBalance.getAvailableAmount());
            }

            if (result.failed()) {
                SellerBalance releasedBalance = sbService.releasePayout(
                        sellerId, payout.getId(), requestPayout.amount()
                );
                payout.setStatus(PayoutStatus.failed);
                payout.setFailureReason(nonBlank(result.failureReason(), "Provider rejected the payout"));
                idempotency.setStatus(IdempotencyKeyStatus.FAILED);
                idempotency.setResponseStatus(422);
                idempotency.setResponseBody(Map.of(
                        "payoutId", payout.getId().toString(),
                        "status", payout.getStatus().name(),
                        "failureReason", payout.getFailureReason()
                ));
                return toDTO(payoutRepository.save(payout), releasedBalance.getAvailableAmount());
            }

            payout.setFailureReason("Provider returned an unknown payout status; reconciliation is required");
            sbService.recordPayoutBalance(sellerId, payout.getId(), reservedBalance.getAvailableAmount());
            return toDTO(payoutRepository.save(payout), reservedBalance.getAvailableAmount());
        } catch (RuntimeException exception) {
            // A transport error may occur after the provider has settled the payout.
            // Keep the balance reserved and the idempotency key processing until reconciled.
            payout.setStatus(PayoutStatus.processing);
            payout.setFailureReason("Provider outcome unknown; reconciliation required before retry");
            sbService.recordPayoutBalance(sellerId, payout.getId(), reservedBalance.getAvailableAmount());
            return toDTO(payoutRepository.save(payout), reservedBalance.getAvailableAmount());
        }
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
