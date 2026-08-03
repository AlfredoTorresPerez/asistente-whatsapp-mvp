package com.asistentewhatsapp.shared.util;

import java.util.regex.Pattern;

public final class PhoneUtils {

	private static final Pattern DIGIT_PATTERN = Pattern.compile("\\D");
	private static final String CHILE_COUNTRY_CODE = "56";
	private static final String CHILE_MOBILE_PREFIX_9 = "9";
	private static final int CHILE_MOBILE_DIGITS = 9;
	private static final int CHILE_LANDLINE_DIGITS = 8;

	private PhoneUtils() {
	}

	public static String normalizeChilePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return null;
		}
		String digits = DIGIT_PATTERN.matcher(phone).replaceAll("");
		if (digits.isEmpty()) {
			return null;
		}
		if (digits.startsWith("00" + CHILE_COUNTRY_CODE)) {
			digits = digits.substring((CHILE_COUNTRY_CODE.length() + 2));
		} else if (digits.startsWith(CHILE_COUNTRY_CODE)) {
			if (digits.length() > CHILE_COUNTRY_CODE.length()
					&& Character.getNumericValue(digits.charAt(CHILE_COUNTRY_CODE.length())) != 0
					&& Character.getNumericValue(digits.charAt(CHILE_COUNTRY_CODE.length())) != 1) {
				digits = digits.substring(CHILE_COUNTRY_CODE.length());
			}
		}
		if (digits.startsWith("0")) {
			digits = digits.substring(1);
		}
		if (digits.length() == CHILE_MOBILE_DIGITS && digits.startsWith(CHILE_MOBILE_PREFIX_9)) {
			return "+" + CHILE_COUNTRY_CODE + digits;
		}
		if (digits.length() == CHILE_MOBILE_DIGITS - 1) {
			digits = CHILE_MOBILE_PREFIX_9 + digits;
			return "+" + CHILE_COUNTRY_CODE + digits;
		}
		if (digits.length() == CHILE_LANDLINE_DIGITS) {
			return "+" + CHILE_COUNTRY_CODE + digits;
		}
		return "+" + CHILE_COUNTRY_CODE + digits;
	}

	public static String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return null;
		}
		String digits = DIGIT_PATTERN.matcher(phone).replaceAll("");
		if (digits.isEmpty()) {
			return null;
		}
		return digits;
	}

	public static boolean isValidChilePhone(String normalizedPhone) {
		if (normalizedPhone == null || !normalizedPhone.startsWith("+56")) {
			return false;
		}
		return normalizeChilePhone(normalizedPhone) != null;
	}
}
