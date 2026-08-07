package com.asistentewhatsapp.aiagents.catalog;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Servicio central de fechas relativas (Fase 8). Resuelve etiquetas como "hoy",
 * "mañana", "pasado mañana", días de la semana, "esta semana" y "próxima
 * semana" a una fecha concreta, y centraliza el mapa de días de la semana que
 * antes vivía duplicado en EntityExtractionService y en los flujos de agenda.
 */
@Component
public class RelativeDateService {

	private static final Map<String, String> WEEKDAYS = weekdays();
	private static final List<String> RELATIVE_DAY_TOKENS = List.of("hoy", "manana", "mañana", "pasado manana",
			"pasado mañana");

	private static volatile RelativeDateService sharedInstance;

	public static RelativeDateService shared() {
		RelativeDateService current = sharedInstance;
		if (current == null) {
			synchronized (RelativeDateService.class) {
				current = sharedInstance;
				if (current == null) {
					current = new RelativeDateService();
					sharedInstance = current;
				}
			}
		}
		return current;
	}

	/** Mapa canónico normalizado -> etiqueta de visualización de día de semana. */
	public Map<String, String> weekdayMap() {
		return WEEKDAYS;
	}

	/** Tokens de día relativo (hoy, mañana, pasado mañana). */
	public List<String> relativeDayTokens() {
		return RELATIVE_DAY_TOKENS;
	}

	public boolean isRelativeDayToken(String normalizedLabel) {
		if (normalizedLabel == null) {
			return false;
		}
		String normalized = LanguageNormalizer.shared().normalize(normalizedLabel);
		return RELATIVE_DAY_TOKENS.contains(normalized) || WEEKDAYS.containsKey(normalized);
	}

	/**
	 * Resuelve una etiqueta de fecha relativa a una fecha concreta.
	 *
	 * @param normalizedLabel
	 *            etiqueta normalizada (p. ej. "manana", "sabado").
	 * @param today
	 *            base de referencia (fecha del día actual).
	 * @return la fecha resuelta, o vacío si la etiqueta no es reconocida.
	 */
	public Optional<LocalDate> resolve(String normalizedLabel, LocalDate today) {
		if (normalizedLabel == null || today == null) {
			return Optional.empty();
		}
		String label = LanguageNormalizer.shared().normalize(normalizedLabel);
		return switch (label) {
			case "hoy" -> Optional.of(today);
			case "manana", "mañana" -> Optional.of(today.plusDays(1));
			case "pasado manana", "pasado mañana" -> Optional.of(today.plusDays(2));
			case "esta semana" -> Optional.of(today);
			case "proxima semana", "la otra semana", "próxima semana" -> Optional.of(today.plusWeeks(1));
			default -> resolveWeekday(label, today);
		};
	}

	private static Optional<LocalDate> resolveWeekday(String normalizedLabel, LocalDate today) {
		String weekday = WEEKDAYS.get(normalizedLabel);
		if (weekday == null) {
			return Optional.empty();
		}
		LocalDate next = today.with(TemporalAdjusters.next(dayOfWeek(weekday)));
		return Optional.of(next);
	}

	private static java.time.DayOfWeek dayOfWeek(String display) {
		return switch (display) {
			case "lunes" -> java.time.DayOfWeek.MONDAY;
			case "martes" -> java.time.DayOfWeek.TUESDAY;
			case "miércoles" -> java.time.DayOfWeek.WEDNESDAY;
			case "jueves" -> java.time.DayOfWeek.THURSDAY;
			case "viernes" -> java.time.DayOfWeek.FRIDAY;
			case "sábado" -> java.time.DayOfWeek.SATURDAY;
			case "domingo" -> java.time.DayOfWeek.SUNDAY;
			default -> throw new IllegalArgumentException("Día de semana desconocido: " + display);
		};
	}

	private static Map<String, String> weekdays() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("lunes", "lunes");
		map.put("martes", "martes");
		map.put("miercoles", "miércoles");
		map.put("jueves", "jueves");
		map.put("viernes", "viernes");
		map.put("sabado", "sábado");
		map.put("domingo", "domingo");
		return Map.copyOf(map);
	}
}