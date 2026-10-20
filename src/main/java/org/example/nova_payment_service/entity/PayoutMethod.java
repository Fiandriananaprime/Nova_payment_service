package org.example.nova_payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.nova_payment_service.dto.PayoutMethodType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_payout_methods")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PayoutMethod {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutMethodType type;

    @Column(length = 100)
    private String label;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "account_encrypted", nullable = false, columnDefinition = "TEXT")
    private String accountEncrypted;

    @Column(name = "masked_account", nullable = false, length = 100)
    private String maskedAccount;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
