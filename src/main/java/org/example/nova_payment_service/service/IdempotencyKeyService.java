package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.IdempotencyKeyStatus;
import org.example.nova_payment_service.entity.IdempotencyKey;
import org.example.nova_payment_service.exception.ConflictException;
import org.example.nova_payment_service.repository.IdempotencyKeyRepository;
import org.example.nova_payment_service.validator.RequestPayout;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class IdempotencyKeyService {
    private final IdempotencyKeyRepository repository;

    private static String calculateRequestHash(UUID payoutMethodId, BigDecimal amount) {
        String payload = payoutMethodId + "|" + amount.stripTrailingZeros().toPlainString();

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private IdempotencyKey create(UUID sellerId, RequestPayout requestPayout,UUID ik){
        UUID pmId = requestPayout.payoutMethodId();
        BigDecimal amount = requestPayout.amount();
        String requestHash = calculateRequestHash(pmId,amount);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

        IdempotencyKey key = IdempotencyKey.builder()
                .sellerId(sellerId)
                .idempotencyKey(ik)
                .requestHash(requestHash)
                .status(IdempotencyKeyStatus.PROCESSING)
                .expiresAt(expiresAt)
                .build();

        return repository.save(key);
    }

    public IdempotencyKey getOrCreate(UUID sellerId, UUID idempotencyKey, RequestPayout requestPayout) {
        String requestHash = calculateRequestHash(requestPayout.payoutMethodId(),requestPayout.amount());

        return repository.findBySellerIdAndIdempotencyKey(sellerId, idempotencyKey)
                .map(existing -> {
                    if (!existing.getRequestHash().equals(requestHash)) throw new ConflictException("Idempotency-Key already used with a different request");
                    if (existing.getExpiresAt() != null &&
                            existing.getExpiresAt().isBefore(Instant.now())) throw new ConflictException("Idempotency-Key has expired");
                    if (existing.getStatus() == IdempotencyKeyStatus.PROCESSING) throw new ConflictException("Request is already processing");

                    return existing;
                })
                .orElseGet(() -> create(sellerId, requestPayout, idempotencyKey));
    }
}
