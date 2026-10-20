package org.example.nova_payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.nova_payment_service.dto.PayoutMethodType;
import org.example.nova_payment_service.dto.PayoutStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutMethodType method;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payout_method_id",
            foreignKey = @ForeignKey(name = "fk_payouts_payout_method")
    )
    private PayoutMethod payoutMethod;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
