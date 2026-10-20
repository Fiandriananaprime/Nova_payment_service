package org.example.nova_payment_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AccountEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountEncryptionService(
            @Value("${security.payout-encryption-key}") String encodedKey) {
        this.key = Base64.getDecoder().decode(encodedKey);

        if (key.length != 32) {
            throw new IllegalArgumentException(
                    "Payout encryption key must be 32 bytes (AES-256)");
        }
    }

    public String encrypt(String account) {
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }

        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, nonce)
            );

            byte[] ciphertext = cipher.doFinal(
                    account.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            byte[] result = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, result, 0, nonce.length);
            System.arraycopy(ciphertext, 0, result, nonce.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt payout account", e);
        }
    }

    public String decrypt(String encryptedAccount) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedAccount);

            if (data.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted account");
            }

            byte[] nonce = Arrays.copyOfRange(data, 0, NONCE_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(
                    data, NONCE_LENGTH, data.length
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, nonce)
            );

            return new String(
                    cipher.doFinal(ciphertext),
                    java.nio.charset.StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt payout account", e);
        }
    }
}