package com.asistentewhatsapp.aiagents.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Normalización de lenguaje centralizada (Fase 7). Sustituye las
 * responsabilidades de TextNormalizer y del método normalize de
 * IntentDetectorService: minúsculas, eliminación de diacríticos, saneamiento de
 * caracteres especiales, errores ortográficos conocidos y corrección de "ora".
 *
 * <p>
 * Es una clase sin estado; se puede inyectar como bean o usar via
 * {@link #shared()} en código construido a mano (tests, agentes viejos).
 */
@Component
public class LanguageNormalizer {

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
	private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9 ]");
	private static final Pattern SPACES = Pattern.compile("\\s+");
	private static final Pattern STANDALONE_ORA = Pattern.compile("(?<![a-z])ora(?![a-z])");

	private static volatile LanguageNormalizer sharedInstance;

	public static LanguageNormalizer shared() {
		LanguageNormalizer current = sharedInstance;
		if (current == null) {
			synchronized (LanguageNormalizer.class) {
				current = sharedInstance;
				if (current == null) {
					current = new LanguageNormalizer();
					sharedInstance = current;
				}
			}
		}
		return current;
	}

	/**
	 * Normalización básica idéntica a TextNormalizer.normalize. No corrige errores
	 * ortográficos.
	 */
	public String normalize(String value) {
		if (value == null) {
			return "";
		}
		return SPACES.matcher(NON_WORD.matcher(DIACRITICS
				.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll(""))
				.replaceAll(" ")).replaceAll(" ").trim();
	}

	/**
	 * Normalización con corrección de errores ortográficos conocidos y de la
	 * palabra "ora" (antes en IntentDetectorService.normalize).
	 */
	public String normalizeWithTypoFix(String value) {
		String result = normalize(value);
		result = result.replace("reserbar", "reservar").replace("recervar", "reservar").replace("resarvar", "reservar")
				.replace("ajendar", "agendar").replace("agndar", "agendar").replace("hroa", "hora");
		return STANDALONE_ORA.matcher(result).replaceAll("hora");
	}

	/** Comprobación de contenido de una subcadena normalizada. */
	public boolean contains(String text, String expected) {
		String normalizedText = normalize(text);
		String normalizedExpected = normalize(expected);
		return !normalizedExpected.isBlank() && normalizedText.contains(normalizedExpected);
	}
}