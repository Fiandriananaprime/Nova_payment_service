package org.example.nova_payment_service.service;

import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.dto.ProviderAccount;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.example.nova_payment_service.dto.PayoutMethodType;
import org.example.nova_payment_service.exception.PaymentMethodNotFound;
import org.example.nova_payment_service.exception.PaymentMethodNotHandled;
import org.example.nova_payment_service.provider.MobileProviderClient;
import org.example.nova_payment_service.repository.PayoutMethodRepository;
import org.example.nova_payment_service.validator.CreatePayoutMethod;
import org.example.nova_payment_service.validator.PayoutUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayoutMethodServiceTest {

    private static final UUID SELLER_ID = UUID.fromString("6f073e22-c12a-4f36-9dd9-64e0d7458c01");
    private static final UUID METHOD_ID = UUID.fromString("45bd1129-5935-427a-bb47-d58b5d09f407");

    private PayoutMethodRepository repository;
    private AccountEncryptionService encryptionService;
    private MobileProviderClient providerClient;
    private PayoutMethodService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayoutMethodRepository.class);
        encryptionService = mock(AccountEncryptionService.class);
        providerClient = mock(MobileProviderClient.class);
        service = new PayoutMethodService(repository, encryptionService, providerClient);
    }

    @Test
    void listsPayoutMethodsUsingMaskedAccountInsteadOfEncryptedValue() {
        PayoutMethod method = payoutMethod(true, true);
        when(repository.findAllBySellerId(SELLER_ID)).thenReturn(List.of(method));

        List<PayoutMethodDto> result = service.findPayoutMethodBySellerId(SELLER_ID);

        assertEquals(1, result.size());
        assertEquals(METHOD_ID, result.get(0).id());
        assertEquals("******4567", result.get(0).maskedAccount());
        assertEquals("Rakoto Jean", result.get(0).accountName());
        assertTrue(result.get(0).isDefault());
        assertTrue(result.get(0).verified());
        assertNotEquals("encrypted-account-value", result.get(0).maskedAccount());
        verify(repository).findAllBySellerId(SELLER_ID);
    }

    @Test
    void createsMvolaPayoutWithNormalizedEncryptedAndMaskedAccount() {
        String normalizedPhone = "0341234567";
        when(providerClient.findAccount("MVOLA", normalizedPhone))
                .thenReturn(new ProviderAccount("MVOLA", normalizedPhone, "Rakoto Jean", "ACTIVE"));
        when(encryptionService.encrypt(normalizedPhone)).thenReturn("encrypted-account-value");
        when(repository.save(any(PayoutMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PayoutMethodDto result = service.createPayoutMethod(
                SELLER_ID,
                new CreatePayoutMethod(
                        PayoutMethodType.mvola,
                        "Main account",
                        "034 123 4567",
                        true
                )
        );

        ArgumentCaptor<PayoutMethod> captor = ArgumentCaptor.forClass(PayoutMethod.class);
        verify(repository).save(captor.capture());
        PayoutMethod saved = captor.getValue();

        assertNotNull(saved.getId());
        assertEquals(SELLER_ID, saved.getSellerId());
        assertEquals("encrypted-account-value", saved.getAccountEncrypted());
        assertEquals("******4567", saved.getMaskedAccount());
        assertEquals("Rakoto Jean", saved.getAccountName());
        assertEquals("******4567", result.maskedAccount());
        assertTrue(result.isDefault());
        assertFalse(result.verified());
        verify(providerClient).findAccount("MVOLA", normalizedPhone);
        verify(encryptionService).encrypt(normalizedPhone);
    }

    @Test
    void resolvesOrangeMoneyFrom032Prefix() {
        String phone = "0321234567";
        when(providerClient.findAccount("ORANGE_MONEY", phone))
                .thenReturn(new ProviderAccount("ORANGE_MONEY", phone, "Rabe Soa", "ACTIVE"));
        when(encryptionService.encrypt(phone)).thenReturn("encrypted-orange-account");
        when(repository.save(any(PayoutMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createPayoutMethod(
                SELLER_ID,
                new CreatePayoutMethod(PayoutMethodType.orange_money, null, phone, false)
        );

        verify(providerClient).findAccount("ORANGE_MONEY", phone);
    }

    @Test
    void rejectsUnsupportedPhonePrefixBeforeCallingDependencies() {
        assertThrows(
                PaymentMethodNotHandled.class,
                () -> service.createPayoutMethod(
                        SELLER_ID,
                        new CreatePayoutMethod(
                                PayoutMethodType.mvola,
                                null,
                                "0331234567",
                                false
                        )
                )
        );

        verifyNoInteractions(providerClient, encryptionService, repository);
    }

    @Test
    void updatesOnlyFieldsPresentAndReturnsMaskedAccount() {
        PayoutMethod method = payoutMethod(true, true);
        when(repository.findByIdAndSellerId(METHOD_ID, SELLER_ID))
                .thenReturn(Optional.of(method));
        when(repository.save(method)).thenReturn(method);

        PayoutMethodDto result = service.updatePayoutMethod(
                SELLER_ID,
                METHOD_ID,
                new PayoutUpdate("Updated label", null, null)
        );

        assertEquals("Updated label", method.getLabel());
        assertTrue(method.isDefault());
        assertTrue(method.isVerified());
        assertEquals("******4567", result.maskedAccount());
        assertNotEquals("encrypted-account-value", result.maskedAccount());
        verify(repository).save(method);
    }

    @Test
    void appliesExplicitFalseValuesDuringPatch() {
        PayoutMethod method = payoutMethod(true, true);
        when(repository.findByIdAndSellerId(METHOD_ID, SELLER_ID))
                .thenReturn(Optional.of(method));
        when(repository.save(method)).thenReturn(method);

        PayoutMethodDto result = service.updatePayoutMethod(
                SELLER_ID,
                METHOD_ID,
                new PayoutUpdate(null, false, false)
        );

        assertFalse(method.isDefault());
        assertFalse(method.isVerified());
        assertFalse(result.isDefault());
        assertFalse(result.verified());
        verify(repository).save(method);
    }

    @Test
    void throwsNotFoundAndDoesNotSaveWhenMethodIsMissingOrOwnedByAnotherSeller() {
        when(repository.findByIdAndSellerId(METHOD_ID, SELLER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentMethodNotFound.class,
                () -> service.updatePayoutMethod(
                        SELLER_ID,
                        METHOD_ID,
                        new PayoutUpdate("Updated label", null, null)
                )
        );

        verify(repository, never()).save(any(PayoutMethod.class));
    }

    private PayoutMethod payoutMethod(boolean isDefault, boolean verified) {
        return PayoutMethod.builder()
                .id(METHOD_ID)
                .sellerId(SELLER_ID)
                .type(PayoutMethodType.mvola)
                .label("Main account")
                .accountName("Rakoto Jean")
                .accountEncrypted("encrypted-account-value")
                .maskedAccount("******4567")
                .isDefault(isDefault)
                .verified(verified)
                .build();
    }
}
