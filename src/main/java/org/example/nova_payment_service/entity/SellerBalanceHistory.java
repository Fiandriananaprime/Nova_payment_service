package org.example.nova_payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_balance_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SellerBalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "payout_id", nullable = false)
    private UUID payoutId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "MGA";

    @Column(name = "recorded_at", nullable = false, columnDefinition = "TIMESTAMPTZ(6)")
    private Instant recordedAt;

}