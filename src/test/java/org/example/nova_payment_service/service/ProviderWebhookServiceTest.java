package org.example.nova_payment_service.service;

import org.example.nova_payment_service.dto.MvolaCallbackRequest;
import org.example.nova_payment_service.dto.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.example.nova_payment_service.repository.ProviderWebhookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderWebhookServiceTest {
    private static final String SECRET = "a".repeat(40);

    @Test
    void acceptsMvolaCallbackAndUpdatesPendingTransaction() {
        ProviderWebhookRepository repository = mock(ProviderWebhookRepository.class);
        when(repository.completeMvolaPayment("completed", "provider-transaction-id", "correlation-id")).thenReturn(1);
        ProviderWebhookService service = new ProviderWebhookService(repository, SECRET);

        ProviderWebhookResponse response = service.receiveMvolaCallback(
                SECRET,
                new MvolaCallbackRequest("completed", "correlation-id", "provider-transaction-id")
        );

        assertEquals("completed", response.status());
        assertEquals(false, response.duplicate());
        verify(repository).completeMvolaPayment("completed", "provider-transaction-id", "correlation-id");
    }

    @Test
    void acceptsOrangeSuccessAndPersistsUsingNotificationToken() {
        ProviderWebhookRepository repository = mock(ProviderWebhookRepository.class);
        when(repository.completeOrangeMoneyPayment(
                eq("completed"),
                eq("provider-transaction-id"),
                anyString()
        )).thenReturn(1);
        ProviderWebhookService service = new ProviderWebhookService(repository, SECRET);

        ProviderWebhookResponse response = service.receiveOrangeNotification(
                new OrangeMoneyNotificationRequest("SUCCESS", "opaque-notification-token", "provider-transaction-id")
        );

        assertEquals("completed", response.status());
        assertEquals(false, response.duplicate());
        verify(repository).completeOrangeMoneyPayment(
                eq("completed"),
                eq("provider-transaction-id"),
                anyString()
        );
    }

    @Test
    void rejectsMvolaCallbackWhenSecretIsWrongWithoutQueryingDatabase() {
        ProviderWebhookRepository repository = mock(ProviderWebhookRepository.class);
        ProviderWebhookService service = new ProviderWebhookService(repository, SECRET);

        assertThrows(ResponseStatusException.class, () -> service.receiveMvolaCallback(
                "wrong-secret",
                new MvolaCallbackRequest("completed", "correlation-id", null)
        ));

        verifyNoInteractions(repository);
    }
}
