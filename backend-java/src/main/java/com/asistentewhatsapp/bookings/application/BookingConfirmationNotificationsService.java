package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.shared.email.AppointmentConfirmationEmailDTO;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.Locale;
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
	private final CompleteAgendaJdbcRepository agendaRepository;
	private final BusinessLocationJdbcRepository locationRepository;
	private final MetaOnboardingRepository metaOnboardingRepository;

	public BookingConfirmationNotificationsService(ReminderSchedulingService reminderSchedulingService,
			BookingEmailService bookingEmailService, AuditService auditService, CalendarSyncService calendarSyncService,
			ChannelDispatchService channelDispatchService, TransactionalEmailService transactionalEmailService,
			CompleteAgendaJdbcRepository agendaRepository, BusinessLocationJdbcRepository locationRepository,
			MetaOnboardingRepository metaOnboardingRepository) {
		this.reminderSchedulingService = reminderSchedulingService;
		this.bookingEmailService = bookingEmailService;
		this.auditService = auditService;
		this.calendarSyncService = calendarSyncService;
		this.channelDispatchService = channelDispatchService;
		this.transactionalEmailService = transactionalEmailService;
		this.agendaRepository = agendaRepository;
		this.locationRepository = locationRepository;
		this.metaOnboardingRepository = metaOnboardingRepository;
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

	public AppointmentConfirmationEmailDTO buildConfirmationEmailDto(ConfirmationLinkRecord link) {
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
		enrichWithCatalogData(dto, link.businessId(), link.locationId(), link.serviceId(), link.durationMinutes());
		return dto;
	}

	public AppointmentConfirmationEmailDTO buildConfirmationEmailDto(ConfirmationBookingRecord booking,
			String confirmationUrl) {
		AppointmentConfirmationEmailDTO dto = new AppointmentConfirmationEmailDTO();
		dto.setPatientName(booking.customerName());
		dto.setServiceName(booking.serviceName() != null ? booking.serviceName() : booking.subject());
		dto.setProfessionalName(booking.professionalName());
		dto.setBranchName(booking.locationName() != null ? booking.locationName() : booking.location());
		dto.setEmail(booking.customerEmail());
		dto.setAppointmentDate(booking.startsAt().toLocalDate().toString());
		dto.setAppointmentTime(booking.startsAt().toLocalTime().toString());
		dto.setConfirmationUrl(confirmationUrl);
		dto.setBookingStatus(booking.bookingStatus());
		enrichWithCatalogData(dto, booking.businessId(), booking.locationId(), booking.serviceId(),
				booking.durationMinutes());
		return dto;
	}

	private void enrichWithCatalogData(AppointmentConfirmationEmailDTO dto, UUID businessId, UUID locationId,
			UUID serviceId, int durationMinutes) {
		dto.setDuration(formatDuration(durationMinutes));
		ServiceRecord service = serviceId != null
				? agendaRepository.findService(businessId, locationId, serviceId)
				: null;
		if (service != null && service.priceBase() != null) {
			NumberFormat clpFormat = NumberFormat.getIntegerInstance(new Locale("es", "CL"));
			dto.setPrice("$" + clpFormat.format(service.priceBase()));
		}
		BusinessLocationRecord locationRecord = locationId != null
				? locationRepository.findActiveById(businessId, locationId)
				: null;
		if (locationRecord != null && locationRecord.address() != null && !locationRecord.address().isBlank()) {
			dto.setAddress(locationRecord.address());
		}
		String channelPhone = metaOnboardingRepository.findCloudApiChannel(businessId)
				.map(ChannelAccountRecord::phoneNumber).orElse(null);
		String displayPhone = channelPhone != null
				? channelPhone
				: (locationRecord != null && locationRecord.phone() != null ? locationRecord.phone() : null);
		if (displayPhone != null && !displayPhone.isBlank()) {
			dto.setPhone(displayPhone);
		}
	}

	private String formatDuration(int minutes) {
		if (minutes < 60) {
			return minutes + " min";
		}
		int hours = minutes / 60;
		int remaining = minutes % 60;
		if (remaining == 0) {
			return hours + "h";
		}
		return hours + "h " + remaining + "min";
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
