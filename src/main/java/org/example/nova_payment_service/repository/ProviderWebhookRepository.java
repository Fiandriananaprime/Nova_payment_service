package org.example.nova_payment_service.repository;

import org.example.nova_payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProviderWebhookRepository extends JpaRepository<PaymentTransaction, UUID> {

    @Modifying
    @Query(value = """
            UPDATE transactions
            SET status = CAST(:status AS transaction_status),
                provider_transaction_reference = :transactionReference,
                updated_at = NOW()
            WHERE method = 'mvola'
              AND provider_reference = :serverCorrelationId
              AND status = 'pending'
            """, nativeQuery = true)
    int completeMvolaPayment(
            @Param("status") String status,
            @Param("transactionReference") String transactionReference,
            @Param("serverCorrelationId") String serverCorrelationId
    );

    @Modifying
    @Query(value = """
            UPDATE transactions
            SET status = CAST(:status AS transaction_status),
                provider_transaction_reference = COALESCE(:transactionReference, provider_transaction_reference),
                updated_at = NOW()
            WHERE method = 'orange_money'
              AND provider_notification_token_hash = :notificationTokenHash
              AND status = 'pending'
            """, nativeQuery = true)
    int completeOrangeMoneyPayment(
            @Param("status") String status,
            @Param("transactionReference") String transactionReference,
            @Param("notificationTokenHash") String notificationTokenHash
    );

    @Query(value = """
            SELECT method::text AS method, status::text AS status
            FROM transactions
            WHERE method = CAST(:method AS transaction_method)
              AND provider_reference = :providerReference
            """, nativeQuery = true)
    Optional<ProviderPaymentState> findStateByProviderReference(
            @Param("method") String method,
            @Param("providerReference") String providerReference
    );

    @Query(value = """
            SELECT method::text AS method, status::text AS status
            FROM transactions
            WHERE method = 'orange_money'
              AND provider_notification_token_hash = :notificationTokenHash
            """, nativeQuery = true)
    Optional<ProviderPaymentState> findStateByNotificationTokenHash(
            @Param("notificationTokenHash") String notificationTokenHash
    );

    interface ProviderPaymentState {
        String getMethod();

        String getStatus();
    }
}
