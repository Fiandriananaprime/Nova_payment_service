package org.example.nova_payment_service.service;

import org.example.nova_payment_service.entity.PhoneVerification;
import org.example.nova_payment_service.provider.SmsService;
import org.example.nova_payment_service.repository.PhoneVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PhoneVerificationService {

    private final PhoneVerificationRepository repository;
    private final SmsService smsService;
    private final String otpSecret;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            PhoneVerificationRepository repository,
            SmsService smsService,
            @Value("${security.otp-secret}") String otpSecret
    ) {
        this.repository = repository;
        this.smsService = smsService;
        this.otpSecret = otpSecret;
    }

    @Transactional
    public void requestCode(UUID userId, String phone) {

        String code = String.format(
                "%06d", secureRandom.nextInt(1_000_000)
        );

        PhoneVerification verification = new PhoneVerification();
        verification.setUserId(userId);
        verification.setPhone(phone);
        verification.setCodeHash(hash(code));
        verification.setExpiresAt(Instant.now().plusSeconds(300));
        verification.setCreatedAt(Instant.now());

        repository.save(verification);

        smsService.send(
                phone,
                "Votre code de vérification Nova Market est : "
                        + code + ". Il expire dans 5 minutes."
        );
    }

    @Transactional
    public boolean verifyCode(UUID userId, String phone, String code) {
        PhoneVerification verification = repository
                .findTopByUserIdAndPhoneAndConsumedFalseOrderByCreatedAtDesc(
                        userId, phone
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Code invalide ou expiré"));

        if (verification.getExpiresAt().isBefore(Instant.now())
                || verification.getAttempts() >= 5) {
            verification.setConsumed(true);
            throw new IllegalArgumentException("Code invalide ou expiré");
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!java.security.MessageDigest.isEqual(
                verification.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8))) {
            repository.save(verification);
            return false;
        }

        verification.setConsumed(true);
        repository.save(verification);

        return true;
    }

    private String hash(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    otpSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            return HexFormat.of().formatHex(
                    mac.doFinal(code.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de traiter le code OTP", e);
        }
    }
}