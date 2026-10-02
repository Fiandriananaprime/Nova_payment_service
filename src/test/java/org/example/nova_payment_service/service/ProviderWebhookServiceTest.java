package org.example.nova_payment_service.service;

import org.example.nova_payment_service.dto.MvolaCallbackRequest;
import org.example.nova_payment_service.dto.OrangeMoneyNotificationRequest;
import org.example.nova_payment_service.dto.ProviderWebhookResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderWebhookServiceTest {
    private static final String SECRET = "a".repeat(40);

    @Test
    void acceptsMvolaCallbackAndUpdatesPendingTransaction() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);
        ProviderWebhookService service = new ProviderWebhookService(jdbcTemplate, SECRET, SECRET);

        ProviderWebhookResponse response = service.receiveMvolaCallback(
                SECRET,
                new MvolaCallbackRequest("completed", "correlation-id", "provider-transaction-id")
        );

        assertEquals("completed", response.status());
        assertEquals(false, response.duplicate());
        verify(jdbcTemplate).update(anyString(), any(), any(), any());
    }

    @Test
    void acceptsOrangeSuccessAndPersistsUsingNotificationToken() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);
        ProviderWebhookService service = new ProviderWebhookService(jdbcTemplate, SECRET, SECRET);

        ProviderWebhookResponse response = service.receiveOrangeNotification(
                new OrangeMoneyNotificationRequest("SUCCESS", "opaque-notification-token", "provider-transaction-id")
        );

        assertEquals("completed", response.status());
        assertEquals(false, response.duplicate());
        verify(jdbcTemplate).update(anyString(), any(), any(), any());
    }

    @Test
    void rejectsMvolaCallbackWhenSecretIsWrongWithoutQueryingDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProviderWebhookService service = new ProviderWebhookService(jdbcTemplate, SECRET, SECRET);

        assertThrows(ResponseStatusException.class, () -> service.receiveMvolaCallback(
                "wrong-secret",
                new MvolaCallbackRequest("completed", "correlation-id", null)
        ));

        verifyNoInteractions(jdbcTemplate);
    }
}
