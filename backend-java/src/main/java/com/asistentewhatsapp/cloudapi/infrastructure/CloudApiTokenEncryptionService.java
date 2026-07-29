package com.asistentewhatsapp.cloudapi.infrastructure;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudApiTokenEncryptionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudApiTokenEncryptionService.class);
	private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;
	private static final int KEY_LENGTH = 256;
	private static final int PBKDF2_ITERATIONS = 65536;
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final byte[] PBKDF2_SALT = "WhatsAppCloudApiTokenEncryption"
			.getBytes(java.nio.charset.StandardCharsets.UTF_8);

	private final SecretKey secretKey;
	private final boolean available;

	public CloudApiTokenEncryptionService(
			@Value("${app.channels.whatsapp-cloud-api.credential-encryption-secret:}") String encryptionSecret) {
		if (encryptionSecret == null || encryptionSecret.isBlank()) {
			LOGGER.warn("credential-encryption-secret not configured; token encryption unavailable");
			this.secretKey = null;
			this.available = false;
			return;
		}
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
			KeySpec spec = new PBEKeySpec(encryptionSecret.toCharArray(), PBKDF2_SALT, PBKDF2_ITERATIONS, KEY_LENGTH);
			SecretKey tmp = factory.generateSecret(spec);
			this.secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
			this.available = true;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to derive AES key from credential-encryption-secret", e);
		}
	}

	public String encrypt(String plainText) {
		if (plainText == null)
			return null;
		if (!available) {
			throw new IllegalStateException(
					"Token encryption is not available. Configure app.channels.whatsapp-cloud-api.credential-encryption-secret.");
		}
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
			LOGGER.error("Failed to encrypt token", e);
			throw new RuntimeException("Failed to encrypt token", e);
		}
	}

	public String decrypt(String encryptedData) {
		if (encryptedData == null)
			return null;
		if (!available) {
			throw new IllegalStateException(
					"Token encryption is not available. Configure app.channels.whatsapp-cloud-api.credential-encryption-secret.");
		}
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
			LOGGER.error("Failed to decrypt token", e);
			throw new RuntimeException("Failed to decrypt token", e);
		}
	}
}