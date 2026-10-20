
package org.example.nova_payment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_balances")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerBalance {

    @Id
    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "available_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableAmount = BigDecimal.ZERO;

    @Column(name = "pending_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "MGA";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SellerBalance(UUID sellerId) {
        this.sellerId = sellerId;
    }
}
