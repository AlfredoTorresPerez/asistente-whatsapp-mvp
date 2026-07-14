package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BookingPhoneObfuscatorTest {

    private final BookingSyncProperties properties = new BookingSyncProperties();
    private final BookingPhoneObfuscator obfuscator = new BookingPhoneObfuscator(properties);

    @Test
    void obfuscateReturnsHmacPrefixedWhenPlaintextDisabled() {
        properties.setPhonePlaintextEnabled(false);
        String result = obfuscator.obfuscate("56950954580");
        assertThat(result).startsWith("h:");
        assertThat(result).hasSize(18);
    }

    @Test
    void obfuscateReturnsPlainPhoneWhenPlaintextEnabled() {
        properties.setPhonePlaintextEnabled(true);
        String result = obfuscator.obfuscate("56950954580");
        assertThat(result).isEqualTo("56950954580");
    }

    @Test
    void obfuscateReturnsNullForNullInput() {
        String result = obfuscator.obfuscate(null);
        assertThat(result).isNull();
    }

    @Test
    void obfuscateReturnsBlankInput() {
        String result = obfuscator.obfuscate("   ");
        assertThat(result).isEqualTo("   ");
    }

    @Test
    void obfuscateStripsWhitespaceWhenPlaintextEnabled() {
        properties.setPhonePlaintextEnabled(true);
        String result = obfuscator.obfuscate(" 56950954580 ");
        assertThat(result).isEqualTo("56950954580");
    }

    @Test
    void obfuscateProducesConsistentDigest() {
        properties.setPhonePlaintextEnabled(false);
        String first = obfuscator.obfuscate("56950954580");
        String second = obfuscator.obfuscate("56950954580");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void toManagementIdReturnsFullHmacWhenPlaintextDisabled() {
        properties.setPhonePlaintextEnabled(false);
        String result = obfuscator.toManagementId("56950954580");
        assertThat(result).doesNotStartWith("h:");
        assertThat(result).hasSize(64);
    }

    @Test
    void toManagementIdReturnsPlainPhoneWhenPlaintextEnabled() {
        properties.setPhonePlaintextEnabled(true);
        String result = obfuscator.toManagementId("56950954580");
        assertThat(result).isEqualTo("56950954580");
    }

    @Test
    void toManagementIdReturnsNullForNullInput() {
        String result = obfuscator.toManagementId(null);
        assertThat(result).isNull();
    }
}
