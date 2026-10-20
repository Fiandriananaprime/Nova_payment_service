package org.example.nova_payment_service.service;

import lombok.RequiredArgsConstructor;
import org.example.nova_payment_service.dto.PayoutMethodDto;
import org.example.nova_payment_service.dto.ProviderAccount;
import org.example.nova_payment_service.entity.PayoutMethod;
import org.example.nova_payment_service.exception.PaymentMethodNotFound;
import org.example.nova_payment_service.exception.PaymentMethodNotHandled;
import org.example.nova_payment_service.provider.MobileProviderClient;
import org.example.nova_payment_service.repository.PayoutMethodRepository;
import org.example.nova_payment_service.validator.CreatePayoutMethod;
import org.example.nova_payment_service.validator.PayoutUpdate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PayoutMethodService {
    private final PayoutMethodRepository repository;
    private final AccountEncryptionService encryptionService;
    private final MobileProviderClient providerClient;

    private String maskAccount(String account) {
        if (account.length() <= 4) {
            return "*".repeat(account.length());
        }

        return "*".repeat(account.length() - 4)
                + account.substring(account.length() - 4);
    }

    public List<PayoutMethodDto> findPayoutMethodBySellerId(UUID sellerId) {
        List<PayoutMethod> payoutMethods = repository.findAllBySellerId(sellerId);

        return payoutMethods.stream().map(
                (p) -> new PayoutMethodDto(
                        p.getId(),
                        p.getType(),
                        p.getLabel(),
                        p.getAccountName(),
                        p.getMaskedAccount(),
                        p.isDefault(),
                        p.isVerified())
        ).toList();
    }

    public PayoutMethodDto createPayoutMethod(
            UUID sellerId,
            CreatePayoutMethod request
    ) {
        String phoneNumber = request.account().replaceAll("\\s+", "");

        String provider = switch (phoneNumber.substring(0, 3)) {
            case "034", "038" -> "MVOLA";
            case "032" -> "ORANGE_MONEY";
            case "037" -> "AIRTEL_MONEY";
            default -> throw new PaymentMethodNotHandled("¨Phone provider not handled");
        };

        ProviderAccount account = providerClient.findAccount(provider, phoneNumber);
        PayoutMethod payoutMethod = repository.save(
                PayoutMethod.builder()
                        .sellerId(sellerId)
                        .type(request.type())
                        .accountName(account.accountName())
                        .label(request.label())
                        .accountEncrypted(encryptionService.encrypt(request.account()))
                        .maskedAccount(maskAccount(request.account()))
                        .isDefault(Boolean.TRUE.equals(request.isDefault()))
                        .verified(false)
                        .build()
        );


        return new PayoutMethodDto(
                payoutMethod.getId(),
                payoutMethod.getType(),
                payoutMethod.getLabel(),
                payoutMethod.getAccountName(),
                payoutMethod.getMaskedAccount(),
                payoutMethod.isDefault(),
                payoutMethod.isVerified()
        );
    }

    public PayoutMethodDto updatePayoutMethod(UUID userId, UUID pmId, PayoutUpdate request) {
        PayoutMethod pm = repository.findByIdAndSellerId(pmId, userId)
                .orElseThrow(() ->
                        new PaymentMethodNotFound("Payout method not found")
                );
        if (request.label() != null) pm.setLabel(request.label());
        if (request.verify() != null) pm.setVerified(request.verify());
        if (request.isDefault() != null) pm.setDefault(request.isDefault());
        repository.save(pm);

        return new PayoutMethodDto(
                pm.getId(),
                pm.getType(),
                pm.getLabel(),
                pm.getAccountName(),
                pm.getAccountEncrypted(),
                pm.isDefault(),
                pm.isVerified()
        );
    }
}