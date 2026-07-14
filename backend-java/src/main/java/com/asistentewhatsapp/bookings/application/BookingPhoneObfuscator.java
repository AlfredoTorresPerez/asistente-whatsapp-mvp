package com.asistentewhatsapp.bookings.application;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class BookingPhoneObfuscator {

    private final BookingSyncProperties properties;

    public BookingPhoneObfuscator(BookingSyncProperties properties) {
        this.properties = properties;
    }

    public String obfuscate(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        if (properties.isPhonePlaintextEnabled()) {
            return phone.trim();
        }
        String digest = hmacSha256(phone.trim(), properties.getHmacSecret());
        return "h:" + digest.substring(0, 16);
    }

    public String toManagementId(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        if (properties.isPhonePlaintextEnabled()) {
            return phone.trim();
        }
        return hmacSha256(phone.trim(), properties.getHmacSecret());
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 not available", e);
        }
    }
}
