package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.shared.email.AppointmentConfirmationEmailDTO;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingConfirmationNotificationsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BookingConfirmationNotificationsService.class);

	private final ReminderSchedulingService reminderSchedulingService;
	private final BookingEmailService bookingEmailService;
	private final AuditService auditService;
	private final CalendarSyncService calendarSyncService;
	private final ChannelDispatchService channelDispatchService;
	private final TransactionalEmailService transactionalEmailService;

	public BookingConfirmationNotificationsService(ReminderSchedulingService reminderSchedulingService,
			BookingEmailService bookingEmailService, AuditService auditService, CalendarSyncService calendarSyncService,
			ChannelDispatchService channelDispatchService, TransactionalEmailService transactionalEmailService) {
		this.reminderSchedulingService = reminderSchedulingService;
		this.bookingEmailService = bookingEmailService;
		this.auditService = auditService;
		this.calendarSyncService = calendarSyncService;
		this.channelDispatchService = channelDispatchService;
		this.transactionalEmailService = transactionalEmailService;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void scheduleReminders(UUID businessId, UUID bookingId, OffsetDateTime startsAt) {
		reminderSchedulingService.scheduleDefaultReminders(businessId, bookingId, startsAt);
		auditService.record(businessId, null, "BOOKING_REMINDER_SCHEDULED", "BOOKING", bookingId,
				"Recordatorios automaticos programados.");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendConfirmationWhatsApp(ConfirmationLinkRecord link) {
		String phone = link.customerPhone();
		if (phone == null || phone.isBlank()) {
			LOGGER.info("CONFIRMED_WHATSAPP_SKIPPED bookingId={} reason=sin-telefono", link.bookingId());
			return;
		}
		String normalized = normalizePhoneForWhatsApp(phone);
		if (normalized == null) {
			LOGGER.warn("CONFIRMED_WHATSAPP_SKIPPED bookingId={} reason=formato-invalido phone={}", link.bookingId(),
					maskPhone(phone));
			auditService.record(link.businessId(), null, "CONFIRMED_WHATSAPP_SKIPPED", "BOOKING", link.bookingId(),
					"WhatsApp omitido: el telefono " + maskPhone(phone) + " no es un numero movil valido.");
			return;
		}
		String body = buildConfirmedMessage(link);
		channelDispatchService
				.dispatch(new ChannelDispatchRequest(link.businessId(), MessageChannelType.WHATSAPP, normalized, body));
		auditService.record(link.businessId(), null, "CONFIRMED_WHATSAPP_SENT", "BOOKING", link.bookingId(),
				"Notificacion de confirmacion enviada por WhatsApp.");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendConfirmationEmail(ConfirmationLinkRecord link) {
		String body = bookingEmailService.buildAppointmentEmailBody(link.customerName(),
				"Tu reserva fue confirmada correctamente.",
				link.serviceName() == null ? link.subject() : link.serviceName(),
				link.startsAt().toLocalDate() + " " + link.startsAt().toLocalTime(),
				link.locationName() == null ? link.location() : link.locationName(), link.professionalName(),
				link.roomName(), link.confirmationUrl(), "Te esperamos en la fecha y hora indicada.");
		bookingEmailService.sendBookingEmail(link.businessId(), link.bookingId(), link.customerEmail(),
				"BOOKING_CONFIRMED", "Tu reserva esta confirmada", body);
		try {
			AppointmentConfirmationEmailDTO dto = buildConfirmationEmailDto(link);
			transactionalEmailService.sendBookingConfirmationEmail(dto);
		} catch (Exception e) {
			LOGGER.warn("CONFIRMED_TRANSACTIONAL_EMAIL_FAILED bookingId={} reason={}", link.bookingId(),
					e.getMessage());
			auditService.record(link.businessId(), null, "CONFIRMED_EMAIL_SEND_FAILED", "BOOKING", link.bookingId(),
					"Correo transaccional fallo: " + e.getMessage());
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void auditRecord(UUID businessId, UUID bookingId, UUID linkId, String previousStatus, String linkStatus,
			boolean requiresDeposit, BigDecimal depositAmount, String paymentStatus, OffsetDateTime startsAt) {
		auditService.record(businessId, null, "BOOKING_CONFIRMED_BY_CUSTOMER_LINK", "BOOKING", bookingId,
				"El cliente confirmo la reserva desde enlace publico.",
				AuditMetadata.of("source", "PUBLIC_LINK", "linkId", linkId, "previousStatus",
						BookingStateMachine.canonical(previousStatus), "newStatus", BookingStateMachine.CONFIRMED,
						"requiresDeposit", requiresDeposit, "depositAmount", depositAmount, "paymentStatus",
						paymentStatus, "linkStatus", linkStatus, "startsAt", startsAt));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void syncCalendar(UUID bookingId, UUID businessId) {
		calendarSyncService.syncConfirmed(bookingId, businessId);
	}

	private String buildConfirmedMessage(ConfirmationLinkRecord link) {
		String locationName = link.locationName() != null ? link.locationName() : link.location();
		String serviceName = link.serviceName() != null ? link.serviceName() : link.subject();
		String dateStr = link.startsAt().toLocalDate().toString();
		String timeStr = link.startsAt().toLocalTime().toString();
		return "Reserva confirmada:\n" + serviceName + "\n" + (locationName != null ? locationName + "\n" : "")
				+ dateStr + " " + timeStr + "\n" + "Te esperamos!";
	}

	private AppointmentConfirmationEmailDTO buildConfirmationEmailDto(ConfirmationLinkRecord link) {
		AppointmentConfirmationEmailDTO dto = new AppointmentConfirmationEmailDTO();
		dto.setPatientName(link.customerName());
		dto.setServiceName(link.serviceName() != null ? link.serviceName() : link.subject());
		dto.setProfessionalName(link.professionalName());
		dto.setBranchName(link.locationName() != null ? link.locationName() : link.location());
		dto.setEmail(link.customerEmail());
		dto.setAppointmentDate(link.startsAt().toLocalDate().toString());
		dto.setAppointmentTime(link.startsAt().toLocalTime().toString());
		dto.setConfirmationUrl(link.confirmationUrl());
		dto.setBookingStatus(link.bookingStatus());
		return dto;
	}

	private String normalizePhoneForWhatsApp(String phone) {
		if (phone == null || phone.isBlank())
			return null;
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() < 10 || digits.length() > 15)
			return null;
		if (digits.startsWith("56") && digits.length() >= 11) {
			if (!digits.startsWith("569"))
				return null;
			return digits;
		}
		if (digits.startsWith("569"))
			return digits;
		if (digits.startsWith("9") && digits.length() == 9)
			return "569" + digits;
		return digits;
	}

	private String maskPhone(String phone) {
		return phone == null || phone.length() <= 4 ? "****" : "****" + phone.substring(phone.length() - 4);
	}
}
