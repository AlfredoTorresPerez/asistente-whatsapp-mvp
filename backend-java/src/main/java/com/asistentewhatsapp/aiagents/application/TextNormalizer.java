package com.asistentewhatsapp.aiagents.application;

import java.text.Normalizer;
import java.util.Locale;

final class TextNormalizer {

	private TextNormalizer() {
	}

	static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	static boolean contains(String text, String expected) {
		String normalizedText = normalize(text);
		String normalizedExpected = normalize(expected);
		return !normalizedExpected.isBlank() && normalizedText.contains(normalizedExpected);
	}
}
