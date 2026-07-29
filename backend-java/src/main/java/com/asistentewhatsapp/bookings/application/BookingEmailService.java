package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.security.application.AuditService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingEmailService {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final AuditService auditService;
	private final boolean enabled;
	private final boolean simulationEnabled;
	private final String fromName;

	public BookingEmailService(NamedParameterJdbcTemplate jdbcTemplate, AuditService auditService,
			@Value("${app.email.enabled:false}") boolean enabled,
			@Value("${app.email.simulation-enabled:true}") boolean simulationEnabled,
			@Value("${app.email.from-name:Centro estetico}") String fromName) {
		this.jdbcTemplate = jdbcTemplate;
		this.auditService = auditService;
		this.enabled = enabled;
		this.simulationEnabled = simulationEnabled;
		this.fromName = fromName;
	}

	@Transactional
	public OffsetDateTime sendBookingEmail(UUID businessId, UUID bookingId, String recipientEmail, String templateKey,
			String subject, String body) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			insertLog(businessId, bookingId, "sin-correo@local", subject, templateKey, "SKIPPED", true,
					"La cita no tiene correo electronico registrado.", body, null);
			auditService.record(businessId, null, "EMAIL_SEND_SKIPPED", "BOOKING", bookingId,
					"Correo omitido porque la cita no tiene email.");
			return null;
		}

		UUID startedLogId = insertLog(businessId, bookingId, recipientEmail, subject, templateKey, "STARTED",
				simulationEnabled || !enabled, null, body, null);
		auditService.record(businessId, null, "EMAIL_SEND_STARTED", "BOOKING", bookingId,
				"Inicio envio de correo " + templateKey + ".");

		OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
		boolean simulated = simulationEnabled || !enabled;
		jdbcTemplate.update("""
				update booking_email_log
				set status = :status,
				    simulation = :simulation,
				    sent_at = :sentAt
				where id = :id
				""",
				new MapSqlParameterSource().addValue("id", startedLogId)
						.addValue("status", simulated ? "SIMULATED" : "SENT").addValue("simulation", simulated)
						.addValue("sentAt", sentAt));
		jdbcTemplate.update("""
				update booking
				set last_email_sent_at = :sentAt
				where business_id = :businessId and id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
				.addValue("sentAt", sentAt));
		auditService.record(businessId, null, "EMAIL_SEND_RESULT", "BOOKING", bookingId,
				simulated ? "Correo simulado registrado correctamente." : "Correo enviado correctamente.");
		return sentAt;
	}

	public String buildAppointmentEmailBody(String greetingName, String actionText, String serviceName,
			String startsAtText, String locationName, String professionalName, String roomName, String primaryUrl,
			String note) {
		return """
				%s

				Hola %s,

				%s

				Servicio: %s
				Fecha y hora: %s
				Sucursal: %s
				Profesional: %s
				Cabina: %s

				Accion principal:
				%s

				%s
				""".formatted(fromName, greetingName == null || greetingName.isBlank() ? "cliente" : greetingName,
				actionText, valueOrFallback(serviceName, "Servicio de agenda"),
				valueOrFallback(startsAtText, "Por confirmar"), valueOrFallback(locationName, "Sucursal por confirmar"),
				valueOrFallback(professionalName, "Por asignar"), valueOrFallback(roomName, "No requerida"),
				valueOrFallback(primaryUrl, "Sin enlace publico"),
				valueOrFallback(note, "Gracias por confiar en nosotros."));
	}

	private UUID insertLog(UUID businessId, UUID bookingId, String recipientEmail, String subject, String templateKey,
			String status, boolean simulation, String failureReason, String body, OffsetDateTime sentAt) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into booking_email_log (
				    id, business_id, booking_id, recipient_email, subject, template_key, status,
				    simulation, failure_reason, body_preview, sent_at
				) values (
				    :id, :businessId, :bookingId, :recipientEmail, :subject, :templateKey, :status,
				    :simulation, :failureReason, :bodyPreview, :sentAt
				)
				""",
				new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
						.addValue("bookingId", bookingId).addValue("recipientEmail", recipientEmail)
						.addValue("subject", subject).addValue("templateKey", templateKey).addValue("status", status)
						.addValue("simulation", simulation).addValue("failureReason", failureReason)
						.addValue("bodyPreview", truncate(body, 2000)).addValue("sentAt", sentAt));
		return id;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private String valueOrFallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
