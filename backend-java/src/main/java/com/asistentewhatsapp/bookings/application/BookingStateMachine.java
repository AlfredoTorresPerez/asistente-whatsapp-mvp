package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class BookingStateMachine {

	public static final String REQUESTED = "SOLICITADA";
	public static final String PENDING_CONFIRMATION = "PENDIENTE_CONFIRMACION";
	public static final String PENDING_PAYMENT = "PENDIENTE_PAGO";
	public static final String CONFIRMED = "CONFIRMADA";
	public static final String IN_SERVICE = "EN_ATENCION";
	public static final String RESCHEDULE_PENDING = "REPROGRAMACION_PENDIENTE";
	public static final String RESCHEDULED = "REPROGRAMADA";
	public static final String CANCELLED = "CANCELADA";
	public static final String CANCELLED_BY_CUSTOMER = "CANCELADA_POR_CLIENTE";
	public static final String EXPIRED = "EXPIRADA";
	public static final String COMPLETED = "COMPLETADA";
	public static final String ATTENDED = COMPLETED;
	public static final String NO_SHOW = "NO_ASISTE";

	private BookingStateMachine() {
	}

	public static String label(String status) {
		String canonical = canonical(status);
		if (canonical == null) {
			return "Sin estado";
		}
		return switch (canonical) {
			case REQUESTED -> "Solicitada";
			case PENDING_CONFIRMATION -> "Pendiente de confirmación";
			case PENDING_PAYMENT -> "Pendiente de pago";
			case CONFIRMED -> "Confirmada";
			case IN_SERVICE -> "En atención";
			case RESCHEDULE_PENDING -> "Reprogramación pendiente";
			case RESCHEDULED -> "Reprogramada";
			case CANCELLED -> "Cancelada";
			case CANCELLED_BY_CUSTOMER -> "Cancelada por cliente";
			case EXPIRED -> "Expirada";
			case COMPLETED -> "Completada";
			case NO_SHOW -> "No asistió";
			default -> canonical != null ? canonical : "Sin estado";
		};
	}

	public static String canonical(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		return switch (status.trim().toUpperCase(Locale.ROOT)) {
			case "REQUESTED", "SOLICITADA" -> REQUESTED;
			case "TEMPORARY", "TEMPORAL", "PENDIENTE_CONFIRMACION" -> PENDING_CONFIRMATION;
			case "PENDING_PAYMENT", "PENDIENTE_PAGO" -> PENDING_PAYMENT;
			case "CONFIRMED", "CONFIRMADA" -> CONFIRMED;
			case "IN_PROGRESS", "EN_ATENCION", "EN_ATENCIÓN" -> IN_SERVICE;
			case "RESCHEDULE_PENDING", "REPROGRAMACION_PENDIENTE" -> RESCHEDULE_PENDING;
			case "RESCHEDULED", "REPROGRAMADA" -> RESCHEDULED;
			case "CANCELLED", "CANCELED", "CANCELADA" -> CANCELLED;
			case "CANCELLED_BY_CUSTOMER", "CANCELADA_POR_CLIENTE" -> CANCELLED_BY_CUSTOMER;
			case "EXPIRED", "RELEASED", "LIBERADA", "EXPIRADA" -> EXPIRED;
			case "COMPLETED", "COMPLETADA", "ATTENDED", "ATENDIDA" -> COMPLETED;
			case "NO_SHOW", "NO_ASISTE" -> NO_SHOW;
			default -> status.trim().toUpperCase(Locale.ROOT);
		};
	}

	public static void assertTransition(String currentStatus, String targetStatus, String action) {
		String current = canonical(currentStatus);
		String target = canonical(targetStatus);
		if (canTransition(current, target)) {
			return;
		}
		throw new ApiException(HttpStatus.CONFLICT, "BOOKING_INVALID_STATE_TRANSITION",
				"La cita no puede " + action + " en su estado actual.",
				Map.of("currentStatus", current == null ? "SIN_ESTADO" : current, "targetStatus",
						target == null ? "SIN_ESTADO" : target));
	}

	public static void assertCanReceiveConfirmationLink(String currentStatus) {
		String current = canonical(currentStatus);
		if (isOpen(current) || PENDING_PAYMENT.equals(current)) {
			return;
		}
		throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_CONFIRMABLE",
				"La cita no puede recibir enlace de confirmacion en su estado actual.",
				Map.of("status", current == null ? "SIN_ESTADO" : current));
	}

	public static void assertCanChange(String currentStatus, String action) {
		String current = canonical(currentStatus);
		if (isOpen(current) || CONFIRMED.equals(current) || RESCHEDULED.equals(current)
				|| PENDING_PAYMENT.equals(current)) {
			return;
		}
		throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_CHANGEABLE",
				"La cita no puede " + action + " en su estado actual.",
				Map.of("status", current == null ? "SIN_ESTADO" : current));
	}

	public static boolean isClosed(String status) {
		String current = canonical(status);
		return CANCELLED.equals(current) || CANCELLED_BY_CUSTOMER.equals(current) || EXPIRED.equals(current)
				|| COMPLETED.equals(current) || NO_SHOW.equals(current);
	}

	private static boolean isOpen(String status) {
		return status == null || REQUESTED.equals(status) || PENDING_CONFIRMATION.equals(status)
				|| RESCHEDULE_PENDING.equals(status);
	}

	private static boolean canTransition(String current, String target) {
		if (target == null) {
			return false;
		}
		if (current == null) {
			return REQUESTED.equals(target) || PENDING_CONFIRMATION.equals(target);
		}
		if (current.equals(target)) {
			return true;
		}
		if (isClosed(current)) {
			return false;
		}
		return switch (target) {
			case PENDING_CONFIRMATION -> REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current);
			case PENDING_PAYMENT -> REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current);
			case CONFIRMED ->
				REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current) || PENDING_PAYMENT.equals(current);
			case IN_SERVICE -> CONFIRMED.equals(current) || RESCHEDULED.equals(current);
			case RESCHEDULE_PENDING -> CONFIRMED.equals(current) || RESCHEDULED.equals(current);
			case RESCHEDULED -> REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current)
					|| PENDING_PAYMENT.equals(current) || CONFIRMED.equals(current)
					|| RESCHEDULE_PENDING.equals(current) || RESCHEDULED.equals(current);
			case CANCELLED -> REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current)
					|| PENDING_PAYMENT.equals(current) || CONFIRMED.equals(current)
					|| RESCHEDULE_PENDING.equals(current) || RESCHEDULED.equals(current);
			case CANCELLED_BY_CUSTOMER -> REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current)
					|| PENDING_PAYMENT.equals(current) || CONFIRMED.equals(current)
					|| RESCHEDULE_PENDING.equals(current) || RESCHEDULED.equals(current);
			case EXPIRED ->
				REQUESTED.equals(current) || PENDING_CONFIRMATION.equals(current) || PENDING_PAYMENT.equals(current);
			case COMPLETED -> CONFIRMED.equals(current) || RESCHEDULED.equals(current) || IN_SERVICE.equals(current);
			case NO_SHOW -> CONFIRMED.equals(current) || RESCHEDULED.equals(current);
			default -> false;
		};
	}
}
