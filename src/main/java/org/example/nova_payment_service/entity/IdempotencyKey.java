package org.example.nova_payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.nova_payment_service.dto.IdempotencyKeyStatus;
import org.example.nova_payment_service.dto.PayoutStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_idempotency_seller_key",
                        columnNames = {"seller_id", "idempotency_key"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @Column(name = "payout_id")
    private UUID payoutId;

    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyKeyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}