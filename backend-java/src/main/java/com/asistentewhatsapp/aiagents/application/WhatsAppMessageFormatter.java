package com.asistentewhatsapp.aiagents.application;

import java.util.List;

public final class WhatsAppMessageFormatter {

	private WhatsAppMessageFormatter() {
	}

	public static String temporaryBookingCreated(String service, String location, String date, String time,
			String confirmationUrl, int expirationMinutes) {
		return "✅ *Reserva temporal creada*\n\n" + "Hola, dejé una *reserva temporal* para ti:\n\n" + "*Servicio:* "
				+ safe(service, "el servicio") + "\n" + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
				+ "*Fecha:* " + safe(date, "la fecha solicitada") + "\n" + "*Hora:* " + safe(time, "la hora solicitada")
				+ "\n\n" + "👉 *Toca o copia este enlace para confirmar tu reserva:*\n\n" + safe(confirmationUrl, "")
				+ "\n\n" + "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
				+ "⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.\n"
				+ "Si no confirmas a tiempo, el cupo puede liberarse.";
	}

	public static String bookingPreview(String service, String location, String date, String time) {
		return "👀 *Vista previa de reserva*\n\n" + "Hay disponibilidad para:\n\n" + "*Servicio:* "
				+ safe(service, "el servicio") + "\n" + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
				+ "*Fecha:* " + safe(date, "la fecha solicitada") + "\n" + "*Hora:* " + safe(time, "la hora solicitada")
				+ "\n\n" + "Esta vista previa *no creó una reserva temporal ni un enlace real*.\n"
				+ "Al enviar la respuesta por WhatsApp se creará la reserva temporal y el enlace de confirmación.";
	}

	public static String bookingConfirmed(String service, String location, String date, String time) {
		return "✅ *Reserva confirmada*\n\n" + "Tu hora quedó confirmada:\n\n" + "*Servicio:* "
				+ safe(service, "el servicio") + "\n" + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
				+ "*Fecha:* " + safe(date, "la fecha solicitada") + "\n" + "*Hora:* " + safe(time, "la hora solicitada")
				+ "\n\n" + "Te esperamos 💆‍♀️";
	}

	public static String confirmationLinkResent(String confirmationUrl, int minutesRemaining) {
		return "🔁 *Reenvío de enlace de confirmación*\n\n"
				+ "Aquí tienes nuevamente tu enlace para confirmar la reserva:\n\n"
				+ "👉 *Toca o copia este enlace para confirmar tu reserva:*\n\n" + safe(confirmationUrl, "") + "\n\n"
				+ "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
				+ "⏳ *Importante:* el enlace vence en *" + formatDuration(minutesRemaining) + "*.\n"
				+ "Si ya venció, puedo ayudarte a generar una nueva reserva temporal.";
	}

	public static String confirmationLinkExpired() {
		return "⏳ *El enlace de confirmación venció*\n\n"
				+ "El enlace anterior ya no está disponible y el cupo pudo liberarse.\n\n"
				+ "Puedo ayudarte a revisar disponibilidad nuevamente para crear una nueva reserva temporal.";
	}

	public static String noAvailability(String service, String location, String date, String requestedTime,
			List<String> alternatives) {
		String base = "⚠️ *Horario no disponible*\n\n" + "No encontré disponibilidad para:\n\n" + "*Servicio:* "
				+ safe(service, "el servicio") + "\n" + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
				+ "*Fecha:* " + safe(date, "la fecha solicitada") + "\n" + "*Hora solicitada:* "
				+ safe(requestedTime, "la hora solicitada") + "\n";
		if (alternatives == null || alternatives.isEmpty()) {
			return base + "\nNo encontré horarios cercanos disponibles. Puedo revisar otro día u otra sucursal.";
		}
		StringBuilder builder = new StringBuilder(base)
				.append("\nPuedo revisar otros horarios cercanos. Opciones disponibles:\n\n");
		int index = 1;
		for (String alternative : alternatives.stream().distinct().limit(3).toList()) {
			builder.append(index++).append(". ").append(alternative).append("\n");
		}
		builder.append("\n¿Cuál prefieres?");
		return builder.toString();
	}

	public static String askService() {
		return askService(java.util.List.of());
	}

	public static String askService(java.util.List<String> activeServices) {
		StringBuilder sb = new StringBuilder(
				"Claro 😊 Te ayudo a reservar.\n\n" + "Para empezar, ¿qué servicio quieres agendar?\n\n");
		if (activeServices != null && !activeServices.isEmpty()) {
			sb.append("Opciones disponibles:\n");
			int maxDisplay = Math.min(activeServices.size(), 8);
			for (int i = 0; i < maxDisplay; i++) {
				sb.append((i + 1)).append(". ").append(activeServices.get(i)).append("\n");
			}
			if (activeServices.size() > maxDisplay) {
				sb.append("\n");
			}
			sb.append("\n");
		}
		sb.append("También puedes escribir el nombre del tratamiento que necesitas.");
		return sb.toString();
	}

	public static String askLocation() {
		return askLocation(java.util.List.of());
	}

	public static String askLocation(java.util.List<String> activeLocations) {
		StringBuilder sb = new StringBuilder("Perfecto 😊 ¿En qué sucursal prefieres atenderte?\n\n");
		if (activeLocations != null && !activeLocations.isEmpty()) {
			sb.append("Opciones:\n");
			for (String loc : activeLocations) {
				sb.append("*").append(loc).append("*\n");
			}
		} else {
			sb.append("Puedes indicarme el nombre de la sucursal.");
		}
		return sb.toString();
	}

	public static String askDate() {
		return "Perfecto 😊 ¿Para qué día te gustaría agendar?";
	}

	public static String askTime() {
		return "Perfecto 😊 ¿A qué hora prefieres asistir?";
	}

	public static String cancellationRequest() {
		return "🛑 *Solicitud de cancelación*\n\n" + "Puedo ayudarte a cancelar tu reserva.\n\n"
				+ "Para hacerlo de forma segura, necesito identificar la cita:\n"
				+ "*Sucursal*, *fecha* u *hora* de la reserva.";
	}

	public static String noActiveBookingsFound() {
		return "📭 *Sin reservas activas*\n\n" + "No encontré reservas futuras activas asociadas a este WhatsApp.\n\n"
				+ "Si deseas agendar una nueva hora, solo dime el servicio, la fecha y la hora que prefieres.";
	}

	public static String singleBookingFound(String service, String date, String time, String location,
			String professional, String status, String duration, String amount) {
		return "📋 *Encontré una reserva activa*\n\n" + "Revisa los datos antes de cancelar:\n\n" + "*Servicio:* "
				+ safe(service, "—") + "\n" + "*Fecha:* " + safe(date, "—") + "\n" + "*Hora:* " + safe(time, "—") + "\n"
				+ "*Sucursal:* " + safe(location, "—") + "\n" + "*Profesional:* " + safe(professional, "Por asignar")
				+ "\n" + "*Estado:* " + safe(status, "—") + "\n"
				+ (duration != null && !duration.isBlank() ? "*Duración:* " + duration + "\n" : "")
				+ (amount != null && !amount.isBlank() ? "*Monto:* " + amount + "\n" : "")
				+ "\n¿Confirmas que deseas cancelar esta reserva?\n\n"
				+ "Responde *Sí* para cancelar o *No* para volver.";
	}

	public static String multipleCancellationCandidates(List<CancellationCandidate> candidates) {
		var sb = new StringBuilder("🛑 *Tienes varias reservas activas - Cancelación*\n\n");
		sb.append("Selecciona la reserva que deseas cancelar:\n\n");
		for (int i = 0; i < candidates.size(); i++) {
			var c = candidates.get(i);
			sb.append("*" + (i + 1) + ". " + safe(c.service, "—") + "*\n");
			sb.append(
					"   " + safe(c.date, "—") + " a las " + safe(c.time, "—") + " en " + safe(c.location, "—") + "\n");
			sb.append("   👉 " + safe(c.url, "") + "\n\n");
		}
		sb.append("_Cada enlace vence en 60 minutos._");
		return sb.toString();
	}

	public static String multipleCancellationCandidatesSingleLink(List<? extends CandidateBase> candidates, String url,
			int expirationMinutes) {
		var sb = new StringBuilder("🛑 *Tienes varias reservas activas - Cancelación*\n\n");
		sb.append("Selecciona la reserva que deseas cancelar:\n\n");
		for (int i = 0; i < candidates.size(); i++) {
			var c = candidates.get(i);
			sb.append("*" + (i + 1) + ". " + safe(c.service(), "—") + "*\n");
			sb.append("   " + safe(c.date(), "—") + " a las " + safe(c.time(), "—") + " en " + safe(c.location(), "—")
					+ "\n\n");
		}
		sb.append("👉 *Toca o copia este enlace para gestionar tus reservas:*\n\n");
		sb.append(safe(url, "") + "\n\n");
		sb.append("_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n");
		sb.append("⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.");
		return sb.toString();
	}

	public static String multipleRescheduleCandidates(List<RescheduleCandidate> candidates) {
		var sb = new StringBuilder("🔄 *Tienes varias reservas activas - Reprogramación*\n\n");
		sb.append("Selecciona la reserva que deseas reprogramar:\n\n");
		for (int i = 0; i < candidates.size(); i++) {
			var c = candidates.get(i);
			sb.append("*" + (i + 1) + ". " + safe(c.service, "—") + "*\n");
			sb.append(
					"   " + safe(c.date, "—") + " a las " + safe(c.time, "—") + " en " + safe(c.location, "—") + "\n");
			sb.append("   👉 " + safe(c.url, "") + "\n\n");
		}
		sb.append("_Cada enlace vence en 60 minutos._");
		return sb.toString();
	}

	public static String multipleRescheduleCandidatesSingleLink(List<? extends CandidateBase> candidates, String url,
			int expirationMinutes) {
		var sb = new StringBuilder("🔄 *Tienes varias reservas activas - Reprogramación*\n\n");
		sb.append("Selecciona la reserva que deseas reprogramar:\n\n");
		for (int i = 0; i < candidates.size(); i++) {
			var c = candidates.get(i);
			sb.append("*" + (i + 1) + ". " + safe(c.service(), "—") + "*\n");
			sb.append("   " + safe(c.date(), "—") + " a las " + safe(c.time(), "—") + " en " + safe(c.location(), "—")
					+ "\n\n");
		}
		sb.append("👉 *Toca o copia este enlace para gestionar tus reservas:*\n\n");
		sb.append(safe(url, "") + "\n\n");
		sb.append("_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n");
		sb.append("⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.");
		return sb.toString();
	}

	public interface CandidateBase {
		String service();
		String date();
		String time();
		String location();
	}

	public record CancellationCandidate(String service, String date, String time, String location,
			String url) implements CandidateBase {
	}
	public record RescheduleCandidate(String service, String date, String time, String location,
			String url) implements CandidateBase {
	}

	public static String bookingCancelledSuccess(String service, String date, String time, String location) {
		return "✅ *Reserva cancelada*\n\n" + "Tu reserva fue cancelada correctamente:\n\n" + "*Servicio:* "
				+ safe(service, "—") + "\n" + "*Fecha:* " + safe(date, "—") + "\n" + "*Hora:* " + safe(time, "—") + "\n"
				+ "*Sucursal:* " + safe(location, "—") + "\n\n"
				+ "El cupo quedó liberado. Si necesitas agendar una nueva hora, solo dímelo.";
	}

	public static String rescheduleRequest() {
		return "🔄 *Solicitud de reprogramación*\n\n" + "Puedo ayudarte a reprogramar tu cita.\n\n"
				+ "Para identificar la *cita actual*, indícame la fecha de la cita o el servicio.\n\n"
				+ "También dime el nuevo *día* u *horario* que prefieres.";
	}

	public static String cancellationLinkGenerated(String service, String date, String time, String location,
			String url, int expirationMinutes) {
		return "🛑 *Solicitud de cancelación*\n\n" + "He encontrado tu reserva:\n\n" + "*Servicio:* "
				+ safe(service, "—") + "\n" + "*Fecha:* " + safe(date, "—") + "\n" + "*Hora:* " + safe(time, "—") + "\n"
				+ "*Sucursal:* " + safe(location, "—") + "\n\n"
				+ "👉 *Toca o copia este enlace para cancelar tu reserva:*\n\n" + safe(url, "") + "\n\n"
				+ "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
				+ "⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.";
	}

	public static String rescheduleLinkGenerated(String service, String date, String time, String location, String url,
			int expirationMinutes) {
		return "🔄 *Reprogramación de reserva*\n\n" + "He encontrado tu reserva:\n\n" + "*Servicio:* "
				+ safe(service, "—") + "\n" + "*Fecha actual:* " + safe(date, "—") + "\n" + "*Hora actual:* "
				+ safe(time, "—") + "\n" + "*Sucursal:* " + safe(location, "—") + "\n\n"
				+ "👉 *Toca o copia este enlace para elegir un nuevo horario:*\n\n" + safe(url, "") + "\n\n"
				+ "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
				+ "⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.";
	}

	public static String bookingLink(String url, boolean isKnownCustomer) {
		if (isKnownCustomer) {
			return "✅ *Reserva en línea*\n\n" + "Hola, ya te tengo registrado 😊\n\n"
					+ "👉 *Toca o copia este enlace para seleccionar servicio, fecha y hora:*\n\n" + safe(url, "")
					+ "\n\n" + "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._";
		}
		return "✅ *Reserva en línea*\n\n" + "Para agendar tu hora, completa tus datos en el siguiente enlace:\n\n"
				+ "👉 *Toca o copia este enlace para reservar:*\n\n" + safe(url, "") + "\n\n"
				+ "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._";
	}

	public static String sensitiveCase() {
		return "🚨 *Te derivaré con una persona del equipo*\n\n" + "Lamento que hayas tenido esa reacción.\n\n"
				+ "Voy a derivarte con una persona del equipo para ayudarte de inmediato.\n\n"
				+ "Si tienes molestias importantes o síntomas intensos, consulta con un profesional de salud.";
	}

	public static String humanHandoff() {
		return "Te derivaré con una persona del equipo para que pueda ayudarte directamente.";
	}

	private static String formatDuration(int minutes) {
		if (minutes >= 60 && minutes % 60 == 0) {
			int hours = minutes / 60;
			return hours == 1 ? "1 hora" : hours + " horas";
		}
		return minutes == 1 ? "1 minuto" : minutes + " minutos";
	}

	private static String safe(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}
}
