package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.catalog.LanguageNormalizer;

/**
 * Facade de compatibilidad hacia {@link LanguageNormalizer} (Fase 7). La lógica
 * de normalización está centralizada en el catálogo; esta clase se mantiene
 * para no tocar a los ~20 consumidores históricos del paquete application.
 */
final class TextNormalizer {

	private static final LanguageNormalizer NORMALIZER = LanguageNormalizer.shared();

	private TextNormalizer() {
	}

	static String normalize(String value) {
		return NORMALIZER.normalize(value);
	}

	static boolean contains(String text, String expected) {
		return NORMALIZER.contains(text, expected);
	}
}