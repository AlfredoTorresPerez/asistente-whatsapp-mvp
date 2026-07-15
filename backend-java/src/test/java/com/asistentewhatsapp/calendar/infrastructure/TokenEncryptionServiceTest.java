package com.asistentewhatsapp.calendar.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenEncryptionService - Cifrado y descifrado de tokens OAuth")
class TokenEncryptionServiceTest {

    private static final String VALID_KEY_B64 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String PLAINTEXT = "my_super_secret_token_value_12345";
    private static final String LOCAL = "local";
    private static final String PRODUCTION = "production";

    @Test
    @DisplayName("Cifrado y descifrado roundtrip con clave Base64 32-byte en local")
    void encryptDecryptRoundtrip() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        String encrypted = service.encrypt(PLAINTEXT);
        assertThat(encrypted).isNotNull().isNotEqualTo(PLAINTEXT);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(PLAINTEXT);
    }

    @Test
    @DisplayName("Descifrar null retorna null")
    void decryptNullReturnsNull() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("Cada cifrado produce ciphertext distinto por IV aleatorio")
    void encryptReturnsDifferentCiphertextEachTime() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        Set<String> results = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            results.add(service.encrypt(PLAINTEXT));
        }
        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("Claves distintas producen ciphertext distinto")
    void differentKeysProduceDifferentCiphertext() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        key2[0] = 1;
        TokenEncryptionService service1 = new TokenEncryptionService(Base64.getEncoder().encodeToString(key1), LOCAL);
        TokenEncryptionService service2 = new TokenEncryptionService(Base64.getEncoder().encodeToString(key2), LOCAL);
        assertThat(service1.encrypt(PLAINTEXT)).isNotEqualTo(service2.encrypt(PLAINTEXT));
    }

    @Test
    @DisplayName("Clave Base64 inválida en producción lanza IllegalStateException")
    void invalidBase64KeyInProductionThrows() {
        assertThatThrownBy(() -> new TokenEncryptionService("not-base64!", PRODUCTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("Clave vacía en producción lanza IllegalStateException")
    void emptyKeyInProductionThrows() {
        assertThatThrownBy(() -> new TokenEncryptionService("", PRODUCTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("Clave con longitud incorrecta en producción lanza IllegalStateException")
    void wrongLengthKeyInProductionThrows() {
        String shortB64 = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new TokenEncryptionService(shortB64, PRODUCTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("Modo local con clave vacía genera clave efímera")
    void ephemeralKeyInLocalMode() throws Exception {
        TokenEncryptionService service = new TokenEncryptionService("", LOCAL);
        Field field = TokenEncryptionService.class.getDeclaredField("secretKey");
        field.setAccessible(true);
        javax.crypto.SecretKey key = (javax.crypto.SecretKey) field.get(service);
        assertThat(key.getEncoded()).hasSize(32);
        assertThat(service.isEphemeral()).isTrue();
    }

    @Test
    @DisplayName("Encrypt con null retorna null")
    void encryptNullReturnsNull() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        assertThat(service.encrypt(null)).isNull();
    }

    @Test
    @DisplayName("Ciphertext incluye IV + datos con longitud mayor al texto original")
    void ciphertextIncludesIvPlusData() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        String encrypted = service.encrypt(PLAINTEXT);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        assertThat(decoded.length).isGreaterThan(PLAINTEXT.length());
    }

    @Test
    @DisplayName("Descifrar datos corruptos lanza RuntimeException")
    void decryptCorruptedDataThrows() {
        TokenEncryptionService service = new TokenEncryptionService(VALID_KEY_B64, LOCAL);
        assertThatThrownBy(() -> service.decrypt("this-is-not-valid-base64!!"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("isEphemeral retorna true con clave efímera y false con clave configurada")
    void isEphemeralReflectsKeySource() {
        assertThat(new TokenEncryptionService("", LOCAL).isEphemeral()).isTrue();
        assertThat(new TokenEncryptionService(VALID_KEY_B64, LOCAL).isEphemeral()).isFalse();
    }
}
