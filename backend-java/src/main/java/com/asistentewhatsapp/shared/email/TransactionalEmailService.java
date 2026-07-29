package com.asistentewhatsapp.shared.email;

import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalEmailService.class);
	private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z")
			.withLocale(Locale.forLanguageTag("es-CL"));

	private final JavaMailSender mailSender;
	private final boolean enabled;
	private final boolean simulationEnabled;
	private final String from;
	private final String fromName;
	private final EmailTemplateRenderer templateRenderer;
	private final JavaMailSender mirrorMailSender;
	private final boolean mirrorEnabled;

	public TransactionalEmailService(JavaMailSender mailSender, EmailTemplateRenderer templateRenderer,
			@Value("${app.email.enabled:false}") boolean enabled,
			@Value("${app.email.simulation-enabled:true}") boolean simulationEnabled,
			@Value("${app.email.from:no-reply@localhost}") String from,
			@Value("${app.email.from-name:Centro estetico}") String fromName,
			@Value("${app.email.mirror.enabled:false}") boolean mirrorEnabled,
			@Value("${app.email.mirror.host:}") String mirrorHost,
			@Value("${app.email.mirror.port:587}") int mirrorPort,
			@Value("${app.email.mirror.username:}") String mirrorUsername,
			@Value("${app.email.mirror.password:}") String mirrorPassword,
			@Value("${app.email.mirror.smtp-auth:true}") boolean mirrorSmtpAuth,
			@Value("${app.email.mirror.starttls-enable:true}") boolean mirrorStartTlsEnable,
			@Value("${app.email.mirror.starttls-required:false}") boolean mirrorStartTlsRequired) {
		this.mailSender = mailSender;
		this.templateRenderer = templateRenderer;
		this.enabled = enabled;
		this.simulationEnabled = simulationEnabled;
		this.from = from;
		this.fromName = fromName;
		this.mirrorEnabled = mirrorEnabled && mirrorHost != null && !mirrorHost.isBlank();
		this.mirrorMailSender = this.mirrorEnabled
				? createMirrorMailSender(mirrorHost, mirrorPort, mirrorUsername, mirrorPassword, mirrorSmtpAuth,
						mirrorStartTlsEnable, mirrorStartTlsRequired)
				: null;
	}

	public DeliveryStatus sendPasswordResetEmail(String to, String userName, String resetUrl, Instant expiresAt,
			ZoneId businessZone) {
		if (to == null || to.isBlank()) {
			throw new IllegalArgumentException("El destinatario del correo de recuperacion es obligatorio.");
		}

		boolean simulated = simulationEnabled || !enabled;
		String recipient = to.trim();
		String safeUserName = userName == null || userName.isBlank() ? "usuario" : userName.trim();
		ZoneId zone = businessZone == null ? ZoneId.of("UTC") : businessZone;
		String expirationText = EXPIRATION_FORMATTER.format(expiresAt.atZone(zone));
		String subject = "Restablece tu contrasena - " + fromName;
		String textBody = buildPasswordResetTextBody(safeUserName, resetUrl, expirationText);
		String htmlBody = buildPasswordResetHtmlBody(safeUserName, resetUrl, expirationText);

		if (simulated) {
			LOGGER.info(
					"PASSWORD_RESET_EMAIL_SIMULATED emailMasked={} resetUrlMasked={} emailEnabled={} simulationEnabled={}",
					maskEmail(recipient), maskResetUrl(resetUrl), enabled, simulationEnabled);
			return DeliveryStatus.SIMULATED;
		}

		try {
			sendMimeMessage(mailSender, recipient, subject, textBody, htmlBody);
			LOGGER.info("PASSWORD_RESET_EMAIL_SENT provider=smtp emailMasked={}", maskEmail(recipient));
			sendMirrorEmail(recipient, subject, textBody, htmlBody, "PASSWORD_RESET");
			return DeliveryStatus.SENT;
		} catch (Exception exception) {
			LOGGER.warn("PASSWORD_RESET_EMAIL_FAILED emailMasked={} reason={}", maskEmail(recipient),
					exception.getClass().getSimpleName());
			throw new MailSendException("No se pudo enviar el correo de recuperacion.", exception);
		}
	}

	private jakarta.mail.internet.InternetAddress fromAddress() throws UnsupportedEncodingException {
		return new jakarta.mail.internet.InternetAddress(from, fromName, "UTF-8");
	}

	private String buildPasswordResetTextBody(String userName, String resetUrl, String expirationText) {
		return """
				Hola %s,

				Recibimos una solicitud para restablecer tu contrasena.

				Abre este enlace para crear una nueva contrasena:
				%s

				El enlace vence el %s y solo puede usarse una vez.

				Si no solicitaste este cambio, puedes ignorar este correo.
				""".formatted(userName, resetUrl, expirationText);
	}

	private String buildPasswordResetHtmlBody(String userName, String resetUrl, String expirationText) {
		String escapedName = htmlEscape(userName);
		String escapedUrl = htmlEscape(resetUrl);
		String escapedExpiration = htmlEscape(expirationText);
		return """
				<!doctype html>
				<html lang="es">
				  <body style="font-family: Arial, sans-serif; color: #172033; line-height: 1.6;">
				    <h1 style="font-size: 20px;">Restablece tu contrasena</h1>
				    <p>Hola %s,</p>
				    <p>Recibimos una solicitud para restablecer tu contrasena.</p>
				    <p>
				      <a href="%s" style="display:inline-block;background:#2453ff;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">
				        Crear nueva contrasena
				      </a>
				    </p>
				    <p>El enlace vence el <strong>%s</strong> y solo puede usarse una vez.</p>
				    <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>
				  </body>
				</html>
				"""
				.formatted(escapedName, escapedUrl, escapedExpiration);
	}

	private String maskResetUrl(String resetUrl) {
		if (resetUrl == null || resetUrl.isBlank()) {
			return "***";
		}
		try {
			URI uri = new URI(resetUrl);
			String query = uri.getQuery();
			if (query == null || !query.contains("token=")) {
				return resetUrl;
			}
			return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
					query.replaceAll("token=([^&]+)", "token=***"), uri.getFragment()).toString();
		} catch (URISyntaxException exception) {
			return "***";
		}
	}

	private String maskEmail(String email) {
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		String localPart = email.substring(0, atIndex);
		String domainPart = email.substring(atIndex);
		return localPart.substring(0, Math.min(2, localPart.length())) + "***" + domainPart;
	}

	public DeliveryStatus sendBookingConfirmationEmail(AppointmentConfirmationEmailDTO dto) {
		if (dto.getEmail() == null || dto.getEmail().isBlank()) {
			return DeliveryStatus.SIMULATED;
		}

		boolean simulated = simulationEnabled || !enabled;
		String recipient = dto.getEmail().trim();
		String businessName = dto.getBusinessName() != null ? dto.getBusinessName() : fromName;
		String subject = "Confirma tu reserva - " + businessName;

		String htmlBody;
		try {
			htmlBody = templateRenderer.render("appointment-confirmation", dto);
			LOGGER.info("PRICE_VERIFY price=[{}] htmlHasDollar=[{}]", dto.getPrice(), htmlBody.contains("$"));
		} catch (RuntimeException e) {
			LOGGER.warn("BOOKING_CONFIRMATION_HTML_RENDER_FAILED reason={}", e.getMessage());
			htmlBody = null;
		}
		String textBody = buildFallbackText(dto.getPatientName(), dto.getServiceName(), dto.getBranchName(),
				dto.getAppointmentDate() + " " + dto.getAppointmentTime(), dto.getDuration(), dto.getProfessionalName(),
				businessName, dto.getConfirmationUrl());

		if (simulated) {
			LOGGER.info("BOOKING_CONFIRMATION_EMAIL_SIMULATED emailMasked={} emailEnabled={} simulationEnabled={}",
					maskEmail(recipient), enabled, simulationEnabled);
			return DeliveryStatus.SIMULATED;
		}

		try {
			sendMimeMessage(mailSender, recipient, subject, textBody, htmlBody);
			LOGGER.info("BOOKING_CONFIRMATION_EMAIL_SENT provider=smtp emailMasked={}", maskEmail(recipient));
			sendMirrorEmail(recipient, subject, textBody, htmlBody, "BOOKING_CONFIRMATION");
			return DeliveryStatus.SENT;
		} catch (Exception exception) {
			LOGGER.warn("BOOKING_CONFIRMATION_EMAIL_FAILED emailMasked={} reason={}", maskEmail(recipient),
					exception.getClass().getSimpleName());
			throw new MailSendException("No se pudo enviar el correo de confirmacion de reserva.", exception);
		}
	}

	public DeliveryStatus sendReminderEmail(String to, String subject, String textBody, String htmlBody) {
		if (to == null || to.isBlank()) {
			return DeliveryStatus.SIMULATED;
		}

		boolean simulated = simulationEnabled || !enabled;
		String recipient = to.trim();

		if (simulated) {
			LOGGER.info("REMINDER_EMAIL_SIMULATED emailMasked={} emailEnabled={} simulationEnabled={}",
					maskEmail(recipient), enabled, simulationEnabled);
			return DeliveryStatus.SIMULATED;
		}

		try {
			sendMimeMessage(mailSender, recipient, subject, textBody, htmlBody);
			LOGGER.info("REMINDER_EMAIL_SENT provider=smtp emailMasked={}", maskEmail(recipient));
			sendMirrorEmail(recipient, subject, textBody, htmlBody, "REMINDER");
			return DeliveryStatus.SENT;
		} catch (Exception exception) {
			LOGGER.warn("REMINDER_EMAIL_FAILED emailMasked={} reason={}", maskEmail(recipient),
					exception.getClass().getSimpleName());
			throw new MailSendException("No se pudo enviar el recordatorio.", exception);
		}
	}

	private String buildFallbackText(String customerName, String serviceName, String locationName, String startsAtText,
			String durationMinutes, String professionalName, String businessName, String confirmationUrl) {
		String linkSection = confirmationUrl != null && !confirmationUrl.isBlank()
				? "\nConfirma tu reserva aqui: %s\n".formatted(confirmationUrl)
				: "\nRecibiras un mensaje por WhatsApp con un enlace para confirmar tu cita y mantener el cupo.\n";
		return """
				%s

				Hola %s,

				Tu reserva fue creada exitosamente.

				Servicio: %s
				Fecha y hora: %s
				Duracion: %s
				Sucursal: %s
				Profesional: %s
				%s
				Si no recibes el mensaje en los proximos minutos, contactanos directamente.

				Gracias por confiar en nosotros.
				""".formatted(businessName != null ? businessName : fromName, customerName,
				valueOrFallback(serviceName, "Servicio de agenda"), valueOrFallback(startsAtText, "Por confirmar"),
				valueOrFallback(durationMinutes, "-"), valueOrFallback(locationName, "Sucursal por confirmar"),
				valueOrFallback(professionalName, "Por asignar"), linkSection);
	}

	private String valueOrFallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private JavaMailSender createMirrorMailSender(String host, int port, String username, String password,
			boolean smtpAuth, boolean startTlsEnable, boolean startTlsRequired) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(port);
		if (username != null && !username.isBlank()) {
			sender.setUsername(username);
		}
		if (password != null && !password.isBlank()) {
			sender.setPassword(password);
		}
		Properties properties = sender.getJavaMailProperties();
		properties.put("mail.smtp.auth", Boolean.toString(smtpAuth));
		properties.put("mail.smtp.starttls.enable", Boolean.toString(startTlsEnable));
		properties.put("mail.smtp.starttls.required", Boolean.toString(startTlsRequired));
		properties.put("mail.smtp.connectiontimeout", "10000");
		properties.put("mail.smtp.timeout", "10000");
		properties.put("mail.smtp.writetimeout", "10000");
		return sender;
	}

	private void sendMimeMessage(JavaMailSender sender, String recipient, String subject, String textBody,
			String htmlBody) throws Exception {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		helper.setTo(recipient);
		helper.setFrom(fromAddress());
		helper.setSubject(subject);
		helper.setText(textBody, htmlBody);
		sender.send(message);
	}

	private void sendMirrorEmail(String recipient, String subject, String textBody, String htmlBody, String eventName) {
		if (!mirrorEnabled || mirrorMailSender == null) {
			return;
		}
		try {
			sendMimeMessage(mirrorMailSender, recipient, subject, textBody, htmlBody);
			LOGGER.info("{}_EMAIL_MIRROR_SENT provider=smtp-mirror emailMasked={}", eventName, maskEmail(recipient));
		} catch (Exception exception) {
			LOGGER.warn("{}_EMAIL_MIRROR_FAILED emailMasked={} reason={}", eventName, maskEmail(recipient),
					exception.getClass().getSimpleName());
		}
	}

	private String htmlEscape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	public enum DeliveryStatus {
		SENT, SIMULATED
	}
}
