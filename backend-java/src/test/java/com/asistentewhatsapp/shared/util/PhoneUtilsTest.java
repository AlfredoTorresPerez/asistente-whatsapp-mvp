package com.asistentewhatsapp.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneUtilsTest {

	@Test
	void normalizesChileMobilePhoneWithLeadingZero() {
		assertThat(PhoneUtils.normalizeChilePhone("0912345678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesChileMobilePhoneWithoutPrefix() {
		assertThat(PhoneUtils.normalizeChilePhone("912345678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesChileMobilePhoneWith56Prefix() {
		assertThat(PhoneUtils.normalizeChilePhone("56912345678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesChileMobilePhoneWithPlusAnd56Prefix() {
		assertThat(PhoneUtils.normalizeChilePhone("+56912345678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesChileMobilePhoneWith0056InternationalPrefix() {
		assertThat(PhoneUtils.normalizeChilePhone("0056912345678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesChileLandlineWith9DigitsByAddingPrefix() {
		assertThat(PhoneUtils.normalizeChilePhone("23456789")).isEqualTo("+56923456789");
	}

	@Test
	void normalizesChileLandline() {
		assertThat(PhoneUtils.normalizeChilePhone("23456789")).isEqualTo("+56923456789");
	}

	@Test
	void normalizesPhoneWithSpacesAndDashes() {
		assertThat(PhoneUtils.normalizeChilePhone("+56 9 1234 5678")).isEqualTo("+56912345678");
	}

	@Test
	void normalizesPhoneWithParentheses() {
		assertThat(PhoneUtils.normalizeChilePhone("(+56) 9 1234-5678")).isEqualTo("+56912345678");
	}

	@Test
	void handlesNullInput() {
		assertThat(PhoneUtils.normalizeChilePhone(null)).isNull();
	}

	@Test
	void handlesBlankInput() {
		assertThat(PhoneUtils.normalizeChilePhone("")).isNull();
		assertThat(PhoneUtils.normalizeChilePhone("   ")).isNull();
	}

	@Test
	void handlesEmptyAfterCleaning() {
		assertThat(PhoneUtils.normalizeChilePhone("()- ")).isNull();
	}

	@Test
	void doesNotStrip56WhenAlreadyHasPlus() {
		assertThat(PhoneUtils.normalizeChilePhone("+56912345678")).isEqualTo("+56912345678");
	}

	@Test
	void isValidChilePhoneReturnsTrueForValidFormat() {
		assertThat(PhoneUtils.isValidChilePhone("+56912345678")).isTrue();
	}

	@Test
	void isValidChilePhoneReturnsFalseForNull() {
		assertThat(PhoneUtils.isValidChilePhone(null)).isFalse();
	}

	@Test
	void isValidChilePhoneReturnsFalseForNonChile() {
		assertThat(PhoneUtils.isValidChilePhone("+15551234567")).isFalse();
	}

	@Test
	void normalizePhoneReturnsDigitsOnly() {
		assertThat(PhoneUtils.normalizePhone("+56 9 1234-5678")).isEqualTo("56912345678");
		assertThat(PhoneUtils.normalizePhone("0912345678")).isEqualTo("0912345678");
	}

	@Test
	void normalizePhoneReturnsNullForEmpty() {
		assertThat(PhoneUtils.normalizePhone("")).isNull();
		assertThat(PhoneUtils.normalizeChilePhone("  ")).isNull();
	}
}
