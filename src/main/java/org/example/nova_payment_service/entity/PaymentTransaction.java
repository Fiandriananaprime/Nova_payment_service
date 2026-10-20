package org.example.nova_payment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.nova_payment_service.dto.PaymentTransactionMethod;
import org.example.nova_payment_service.dto.PaymentTransactionStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class PaymentTransaction {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_method")
    private PaymentTransactionMethod method;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private PaymentTransactionStatus status;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "provider_reference")
    private String providerReference;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "provider_notification_token_hash", length = 64)
    private String providerNotificationTokenHash;

    @Column(name = "provider_transaction_reference")
    private String providerTransactionReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }
}
