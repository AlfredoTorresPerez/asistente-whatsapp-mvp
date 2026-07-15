package com.asistentewhatsapp.calendar.infrastructure;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenEncryptionService.class);
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final boolean ephemeral;

    public TokenEncryptionService(
            @Value("${app.calendar.encryption-secret:}") String encryptionSecret,
            @Value("${app.environment:local}") String environment) {
        boolean isDevelopment = "local".equals(environment) || "test".equals(environment);

        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            if (!isDevelopment) {
                throw new IllegalStateException(
                        "APP_CALENDAR_ENCRYPTION_SECRET is not configured. " +
                        "A 32-byte Base64-encoded key is required in production.");
            }
            byte[] keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            this.ephemeral = true;
            LOGGER.warn("CALENDAR_ENCRYPTION_SECRET_NOT_CONFIGURED using ephemeral key. Tokens will not survive restart.");
        } else {
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(encryptionSecret.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "APP_CALENDAR_ENCRYPTION_SECRET is not valid Base64. " +
                        "Provide a Base64-encoded 32-byte key.", e);
            }
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "APP_CALENDAR_ENCRYPTION_SECRET decoded length is " + keyBytes.length +
                        ", expected 32 bytes. Provide a Base64-encoded 32-byte key.");
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            this.ephemeral = false;
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String encryptedData) {
        if (encryptedData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }

    public boolean isEphemeral() {
        return ephemeral;
    }
}
