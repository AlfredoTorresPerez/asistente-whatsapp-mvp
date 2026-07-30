package com.asistentewhatsapp.landing.application;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityRequest;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.aiagents.application.WhatsAppMessageFormatter;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.bookings.application.BookingConfirmationProperties;
import com.asistentewhatsapp.bookings.application.BookingEmailService;
import com.asistentewhatsapp.bookings.application.BookingPolicyService;
import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.customerbookings.application.CustomerBookingService;
import com.asistentewhatsapp.landing.api.CreatePublicBookingRequest;
import com.asistentewhatsapp.landing.api.PublicCustomerInfoResponse;
import com.asistentewhatsapp.landing.api.LandingLocationItemResponse;
import com.asistentewhatsapp.landing.api.LandingPageResponse;
import com.asistentewhatsapp.landing.api.LandingPageResponse.LandingCompanyResponse;
import com.asistentewhatsapp.landing.api.LandingServiceItemResponse;
import com.asistentewhatsapp.landing.api.WhatsAppEntryResponse;
import com.asistentewhatsapp.landing.api.PublicCategoryResponse;
import com.asistentewhatsapp.landing.api.PublicServiceBranchResponse;
import com.asistentewhatsapp.landing.api.PublicServiceDetailResponse;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.security.domain.BusinessEntity;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.bookings.application.BookingReceiptService;
import com.asistentewhatsapp.bookings.application.BookingValidationService;
import com.asistentewhatsapp.bookings.application.ReminderSchedulingService;
import com.asistentewhatsapp.bookings.application.BookingValidationService.CreateBookingCustomerData;
import com.asistentewhatsapp.bookings.application.BookingValidationService.ValidateBookingRequest;
import com.asistentewhatsapp.bookings.application.BookingValidationService.ValidationContext;
import com.asistentewhatsapp.shared.email.AppointmentConfirmationEmailDTO;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicLandingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PublicLandingService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final BusinessRepository businessRepository;
	private final AestheticCenterJdbcRepository aestheticRepository;
	private final BusinessLocationJdbcRepository locationRepository;
	private final MetaOnboardingRepository metaOnboardingRepository;
	private final CompleteAgendaJdbcRepository agendaRepository;
	private final BookingEmailService bookingEmailService;
	private final TransactionalEmailService transactionalEmailService;
	private final BookingConfirmationJdbcRepository bookingConfirmationRepository;
	private final TokenHashService tokenHashService;
	private final ChannelDispatchService channelDispatchService;
	private final BookingConfirmationProperties bookingConfirmationProperties;
	private final AuditService auditService;
	private final BookingValidationService bookingValidationService;
	private final BookingReceiptService bookingReceiptService;
	private final CustomerBookingService customerBookingService;
	private final ReminderSchedulingService reminderSchedulingService;
	private final BookingPolicyService bookingPolicyService;
	private final String frontendPublicBaseUrl;

	public PublicLandingService(BusinessRepository businessRepository,
			AestheticCenterJdbcRepository aestheticRepository, BusinessLocationJdbcRepository locationRepository,
			MetaOnboardingRepository metaOnboardingRepository, CompleteAgendaJdbcRepository agendaRepository,
			BookingEmailService bookingEmailService, TransactionalEmailService transactionalEmailService,
			BookingConfirmationJdbcRepository bookingConfirmationRepository, TokenHashService tokenHashService,
			ChannelDispatchService channelDispatchService, BookingConfirmationProperties bookingConfirmationProperties,
			AuditService auditService, BookingValidationService bookingValidationService,
			BookingReceiptService bookingReceiptService, CustomerBookingService customerBookingService,
			ReminderSchedulingService reminderSchedulingService, BookingPolicyService bookingPolicyService,
			@Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendPublicBaseUrl) {
		this.businessRepository = businessRepository;
		this.aestheticRepository = aestheticRepository;
		this.locationRepository = locationRepository;
		this.metaOnboardingRepository = metaOnboardingRepository;
		this.agendaRepository = agendaRepository;
		this.bookingEmailService = bookingEmailService;
		this.transactionalEmailService = transactionalEmailService;
		this.bookingConfirmationRepository = bookingConfirmationRepository;
		this.tokenHashService = tokenHashService;
		this.channelDispatchService = channelDispatchService;
		this.bookingConfirmationProperties = bookingConfirmationProperties;
		this.auditService = auditService;
		this.bookingValidationService = bookingValidationService;
		this.bookingReceiptService = bookingReceiptService;
		this.customerBookingService = customerBookingService;
		this.reminderSchedulingService = reminderSchedulingService;
		this.bookingPolicyService = bookingPolicyService;
		this.frontendPublicBaseUrl = frontendPublicBaseUrl;
	}

	@Transactional(readOnly = true)
	public LandingPageResponse landing() {
		return landing(null, null, null);
	}

	@Transactional(readOnly = true)
	public LandingPageResponse landing(UUID preferredLocationId, Double latitude, Double longitude) {
		BusinessEntity business = findDefaultBusiness();
		UUID businessId = business.getId();

		List<LandingServiceItemResponse> services = aestheticRepository
				.findServices(businessId, 0, 100, null, null, true).items().stream()
				.map(s -> new LandingServiceItemResponse(s.id(), s.name(), s.description(), s.categoryCode(),
						s.categoryName(), s.durationMinutes(), s.priceBase(), s.professionalRequired(),
						s.requiresPriorEvaluation(), s.requiresInformedConsent()))
				.toList();

		List<LandingLocationItemResponse> locations = locationRepository.findActive(businessId).stream()
				.sorted(locationComparator(preferredLocationId, latitude, longitude))
				.map(l -> new LandingLocationItemResponse(l.id(), l.name(), l.address(), l.city(), l.commune(),
						l.phone(), l.whatsappNumber(), l.timezone(), l.latitude(), l.longitude(),
						l.dailyBookingCapacity(), distanceKm(latitude, longitude, l.latitude(), l.longitude()),
						l.id().equals(preferredLocationId)))
				.toList();

		return new LandingPageResponse(new LandingCompanyResponse(business.getCompanyName(), business.getBusinessName(),
				business.getTimezone(), business.getCurrency(), business.getContactEmail(), business.getSupportPhone(),
				business.getAddress()), services, locations);
	}

	@Transactional(readOnly = true)
	public AgendaAvailabilityResponse availability(AgendaAvailabilityRequest request) {
		UUID businessId = findDefaultBusiness().getId();
		LocationRecord location = agendaRepository.findLocation(businessId, request.locationId());
		CompleteAgendaJdbcRepository.ServiceRecord service = agendaRepository.findService(businessId,
				request.locationId(), request.serviceId());
		int dayOfWeek = request.date().getDayOfWeek().getValue();

		if (agendaRepository.isHoliday(businessId, request.locationId(), request.date())) {
			return emptyAvailability(location, service, request.date());
		}

		List<TimeWindowRecord> businessHours = agendaRepository.findBusinessHours(businessId, request.locationId(),
				dayOfWeek);
		if (businessHours.isEmpty()) {
			return emptyAvailability(location, service, request.date());
		}

		List<ProfessionalRecord> professionals = agendaRepository.findProfessionalCandidates(businessId,
				request.locationId(), request.serviceId(), request.professionalId());
		if (professionals.isEmpty()) {
			return emptyAvailability(location, service, request.date());
		}

		List<RoomRecord> rooms = service.requiresRoom()
				? agendaRepository.findRoomCandidates(businessId, request.locationId(), request.serviceId(),
						request.roomId())
				: List.of(new RoomRecord(null, null));
		if (rooms.isEmpty()) {
			return emptyAvailability(location, service, request.date());
		}

		int limit = normalizeLimit(request.maxSlots());
		List<AgendaSlotResponse> slots = new ArrayList<>();
		for (ProfessionalRecord professional : professionals) {
			List<TimeWindowRecord> professionalHours = agendaRepository.findProfessionalHours(businessId,
					request.locationId(), professional.id(), dayOfWeek);
			if (professionalHours.isEmpty()) {
				continue;
			}
			for (RoomRecord room : rooms) {
				collectSlots(businessId, location, service, professional, room, request, businessHours,
						professionalHours, slots, limit);
				if (slots.size() >= limit) {
					break;
				}
			}
			if (slots.size() >= limit) {
				break;
			}
		}
		Map<String, AgendaSlotResponse> unique = new LinkedHashMap<>();
		for (AgendaSlotResponse s : slots) {
			unique.putIfAbsent(s.startsAt() + "|" + s.endsAt(), s);
		}
		slots = new ArrayList<>(unique.values());
		slots.sort(Comparator.comparing(AgendaSlotResponse::startsAt));
		if (slots.size() > limit) {
			slots = slots.subList(0, limit);
		}
		return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(),
				request.date(), service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), slots);
	}

	public PublicCustomerInfoResponse getCustomerInfo(String token) {
		try {
			return customerBookingService.getCustomerInfoByToken(token);
		} catch (Exception e) {
			return new PublicCustomerInfoResponse(null, null, null, null, null);
		}
	}

	@Transactional
	public UUID createBooking(CreatePublicBookingRequest request) {
		UUID businessId = findDefaultBusiness().getId();
		BusinessEntity business = businessRepository.findById(businessId).orElseThrow();
		LocationRecord location = agendaRepository.findLocation(businessId, request.locationId());
		CompleteAgendaJdbcRepository.ServiceRecord service = agendaRepository.findService(businessId,
				request.locationId(), request.serviceId());

		UUID professionalId = request.professionalId();
		String professionalName = null;
		if (professionalId == null) {
			List<ProfessionalRecord> candidates = agendaRepository.findProfessionalCandidates(businessId,
					request.locationId(), request.serviceId(), null);
			if (candidates.isEmpty()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "NO_PROFESSIONAL_AVAILABLE",
						"No hay profesionales disponibles.", java.util.Map.of());
			}
			professionalId = candidates.getFirst().id();
			professionalName = candidates.getFirst().name();
		} else {
			List<ProfessionalRecord> candidates = agendaRepository.findProfessionalCandidates(businessId,
					request.locationId(), request.serviceId(), professionalId);
			if (!candidates.isEmpty()) {
				professionalName = candidates.getFirst().name();
			}
		}
		UUID roomId = service.requiresRoom()
				? agendaRepository.findRoomCandidates(businessId, request.locationId(), request.serviceId(), null)
						.stream().findFirst().map(RoomRecord::id).orElse(null)
				: null;
		if (service.requiresRoom() && roomId == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "NO_ROOM_AVAILABLE", "No hay cabinas disponibles.",
					java.util.Map.of());
		}

		OffsetDateTime startsAt = request.startsAt();
		ZoneId locationZone = BookingValidationService.resolveLocationZone(location);
		String customerPhone = normalizePhone(request.customerPhone());
		String idempotencyKey = bookingOperationIdempotencyKey("PUBLIC_BOOKING_CREATE", request.idempotencyKey(),
				businessId, null, null, customerPhone, location.id(), service.id(), professionalId, roomId, startsAt);
		String requestHash = bookingOperationRequestHash("PUBLIC_BOOKING_CREATE", businessId, null, null, customerPhone,
				location.id(), service.id(), professionalId, roomId, startsAt, service.durationMinutes());
		UUID idempotentBookingId = resolveIdempotentPublicBooking(businessId, idempotencyKey, requestHash);
		if (idempotentBookingId != null) {
			return idempotentBookingId;
		}
		validateConsentRequirements(service, request.informedConsentAccepted(), request.customerBirthDate(),
				request.guardianName(), request.guardianPhone(), startsAt, location);

		CreateBookingCustomerData customerData = new CreateBookingCustomerData(request.customerName(),
				request.customerPhone(), request.customerEmail());
		ValidateBookingRequest validateReq = new ValidateBookingRequest(request.serviceId(), request.locationId(),
				professionalId, roomId, startsAt, customerData, null,
				bookingConfirmationProperties.getMinMinutesAhead());

		ValidationContext vctx = new ValidationContext(businessId, validateReq);
		vctx.setLocation(location);
		vctx.setService(service);
		vctx.setZone(locationZone);
		vctx.setStartsAt(startsAt);
		vctx.setProfessionalId(professionalId);
		vctx.setRoomId(roomId);
		bookingValidationService.validateAll(vctx);
		bookingValidationService.throwIfErrors(vctx);

		CompleteAgendaJdbcRepository.CustomerRecord customer = agendaRepository.findOrCreateCustomer(businessId, null,
				request.customerName(), request.customerPhone(), request.customerEmail());

		UUID existingBooking = agendaRepository.findActiveBookingByCustomerProfessionalAndStart(businessId,
				customer.id(), professionalId, startsAt);
		if (existingBooking != null) {
			LOGGER.info(
					"Active booking already exists for customer={} professional={} startsAt={}, returning existing bookingId={}",
					customer.id(), professionalId, startsAt, existingBooking);
			return existingBooking;
		}

		OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(60);
		OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
		UUID bookingId = agendaRepository.insertTemporaryBooking(businessId, customer.id(), null, null, null,
				service.name(), location.id(), service.id(), professionalId, roomId, startsAt, endsAt,
				service.durationMinutes(), expiresAt, service.requiresDeposit(), service.depositAmount(),
				request.notes());
		recordConsentIfPresent(businessId, bookingId, service, request.informedConsentAccepted(),
				request.customerBirthDate(), request.guardianName(), request.guardianPhone());
		agendaRepository.completeBookingOperationIdempotency(businessId, "PUBLIC_BOOKING_CREATE", idempotencyKey,
				requestHash, bookingId);

		try {
			UUID receiptId = bookingReceiptService.generateReceipt(businessId, bookingId);
			auditService.record(businessId, null, "BOOKING_RECEIPT_GENERATED", "BOOKING", bookingId,
					"Comprobante de reserva generado desde landing publica.",
					AuditMetadata.of("receiptId", receiptId, "receiptSummary",
							bookingReceiptService.formatReceiptSummary(businessId, bookingId, receiptId)));
		} catch (RuntimeException e) {
			LOGGER.warn("Could not generate booking receipt", e);
		}

		schedulePostCreationReminders(businessId, bookingId, startsAt, location, service);

		auditService.record(businessId, null, "BOOKING_CREATED", "BOOKING", bookingId,
				"Reserva creada desde landing publica.",
				AuditMetadata.of("locationId", location.id(), "serviceId", service.id(), "professionalId",
						professionalId, "roomId", roomId, "startsAt", startsAt, "endsAt", endsAt, "durationMinutes",
						service.durationMinutes(), "requiresDeposit", service.requiresDeposit(), "customerPhone",
						maskPhone(request.customerPhone())));

		String confirmationUrl = generateConfirmationLink(businessId, bookingId, service.requiresDeposit());

		String token = confirmationUrl.substring(confirmationUrl.lastIndexOf('/') + 1);
		String rescheduleUrl = frontendPublicBaseUrl + "/reservas/reprogramar/" + token;
		String cancelUrl = frontendPublicBaseUrl + "/reservas/cancelar/" + token;

		notifyCenter(businessId, location, service, professionalName, customer, startsAt, bookingId);

		if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
			sendWhatsAppConfirmationLink(businessId, bookingId, location.name(), service.name(), professionalName,
					startsAt, request.customerName(), request.customerPhone(), service.durationMinutes(),
					confirmationUrl);
		}

		if (request.customerEmail() != null && !request.customerEmail().isBlank()) {
			sendPostCreationEmail(business, location, service, professionalName, startsAt, bookingId, confirmationUrl,
					rescheduleUrl, cancelUrl, request.customerName(), request.customerEmail(), businessId);
		}

		return bookingId;
	}

	private void schedulePostCreationReminders(UUID businessId, UUID bookingId, OffsetDateTime startsAt,
			LocationRecord location, CompleteAgendaJdbcRepository.ServiceRecord service) {
		try {
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
			agendaRepository.insertReminder(businessId, bookingId, "CONFIRMATION", now.plusMinutes(1));
			reminderSchedulingService.scheduleDefaultReminders(businessId, bookingId, startsAt);
		} catch (RuntimeException e) {
			LOGGER.warn("Could not schedule post-creation reminders", e);
		}

	}

	private void notifyCenter(UUID businessId, LocationRecord location,
			CompleteAgendaJdbcRepository.ServiceRecord service, String professionalName,
			CompleteAgendaJdbcRepository.CustomerRecord customer, OffsetDateTime startsAt, UUID bookingId) {
		try {
			String message = "Nueva reserva #" + bookingId.toString().substring(0, 8).toUpperCase() + ": "
					+ customer.displayName() + " - " + service.name() + " en " + location.name() + " - "
					+ (professionalName != null ? professionalName + " - " : "") + startsAt.toLocalDate() + " "
					+ startsAt.toLocalTime();
			auditService.record(businessId, null, "BOOKING_CENTER_NOTIFIED", "BOOKING", bookingId,
					"Notificacion de nueva reserva enviada al centro.", AuditMetadata.of("message", message));
		} catch (RuntimeException e) {
			LOGGER.warn("Could not notify center about new booking", e);
		}
	}

	private void sendPostCreationEmail(BusinessEntity business, LocationRecord location,
			CompleteAgendaJdbcRepository.ServiceRecord service, String professionalName, OffsetDateTime startsAt,
			UUID bookingId, String confirmationUrl, String rescheduleUrl, String cancelUrl, String customerName,
			String customerEmail, UUID businessId) {
		String startsAtText = startsAt.toLocalDate() + " " + startsAt.toLocalTime();
		String businessName = business.getBusinessName();
		String body = bookingEmailService.buildAppointmentEmailBody(customerName,
				"Tu reserva fue creada exitosamente. Recibiras un enlace por WhatsApp para confirmar la cita.",
				service.name(), startsAtText, location.name(), professionalName, null, null,
				"Gracias por confiar en nosotros.");
		bookingEmailService.sendBookingEmail(businessId, bookingId, customerEmail, "BOOKING_CREATED_PUBLIC",
				"Reserva creada - " + businessName, body);

		AppointmentConfirmationEmailDTO emailDto = new AppointmentConfirmationEmailDTO();
		emailDto.setEmail(customerEmail);
		emailDto.setPatientName(customerName);
		emailDto.setBusinessName(businessName);
		emailDto.setServiceName(service.name());
		emailDto.setBranchName(location.name());
		emailDto.setAppointmentDate(startsAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
		emailDto.setAppointmentTime(startsAt.toLocalTime().toString());
		emailDto.setDuration(formatDuration(service.durationMinutes()));
		emailDto.setProfessionalName(professionalName);
		emailDto.setBookingStatus("Pendiente de confirmacion");
		emailDto.setConfirmationUrl(confirmationUrl);
		emailDto.setRescheduleUrl(rescheduleUrl);
		emailDto.setCancelUrl(cancelUrl);
		emailDto.setReservationCode(bookingId.toString().substring(0, 8).toUpperCase());

		BusinessLocationRecord locationRecord = locationRepository.findActiveById(businessId, location.id());
		if (locationRecord != null) {
			String addr = locationRecord.address() != null ? locationRecord.address() : business.getAddress();
			emailDto.setAddress(addr);
			if (addr != null && !addr.isBlank()) {
				emailDto.setGoogleMapsUrl(
						"https://maps.google.com/?q=" + URLEncoder.encode(addr, StandardCharsets.UTF_8));
			}
		}

		String channelPhone = metaOnboardingRepository.findCloudApiChannel(businessId)
				.map(ChannelAccountRecord::phoneNumber).orElse(null);
		String displayPhone = channelPhone != null
				? channelPhone
				: (locationRecord != null && locationRecord.phone() != null
						? locationRecord.phone()
						: business.getSupportPhone());
		emailDto.setPhone(displayPhone);
		if (displayPhone != null && !displayPhone.isBlank()) {
			emailDto.setWhatsappUrl("https://wa.me/" + displayPhone.replaceAll("[^0-9]", ""));
		}

		emailDto.setReservationDetailsUrl(frontendPublicBaseUrl + "/reservas/" + bookingId);

		NumberFormat clpFormat = NumberFormat.getIntegerInstance(new java.util.Locale("es", "CL"));
		if (service.priceBase() != null) {
			emailDto.setPrice("$" + clpFormat.format(service.priceBase()));
		}

		try {
			var aestheticService = aestheticRepository.findService(businessId, service.id());
			if (aestheticService != null) {
				emailDto.setServiceCategory(aestheticService.categoryName());
				emailDto.setServiceDescription(aestheticService.description());
				emailDto.setAftercareInstructions(aestheticService.aftercareRecommendations());
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Could not fetch aesthetic service details for email", e);
		}

		try {
			transactionalEmailService.sendBookingConfirmationEmail(emailDto);
		} catch (RuntimeException e) {
			auditService.record(businessId, null, "BOOKING_CONFIRMATION_EMAIL_SEND_FAILED", "BOOKING", bookingId,
					"Fallo envio de correo de confirmacion desde landing: "
							+ (e.getMessage() != null ? e.getMessage() : "sin detalle"),
					AuditMetadata.of("error", e.getMessage()));
		}
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() < 6)
			return phone;
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
	}

	@Transactional(readOnly = true)
	public List<PublicCategoryResponse> categories() {
		UUID businessId = findDefaultBusiness().getId();
		var categories = aestheticRepository.findServiceCategories(businessId, 0, 100, true);
		var services = aestheticRepository.findServices(businessId, 0, 1000, null, null, true).items();
		return categories.items().stream().map(cat -> {
			int count = (int) services.stream().filter(s -> cat.code().equals(s.categoryCode())).count();
			return new PublicCategoryResponse(cat.id(), cat.code(), cat.name(), cat.description(), cat.active(), count,
					cat.displayOrder());
		}).sorted(Comparator
				.comparing(PublicCategoryResponse::displayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(PublicCategoryResponse::name)).toList();
	}

	@Transactional(readOnly = true)
	public List<LandingServiceItemResponse> servicesByCategory(String categoryCode) {
		UUID businessId = findDefaultBusiness().getId();
		return aestheticRepository.findServices(businessId, 0, 100, null, categoryCode, true).items().stream()
				.map(s -> new LandingServiceItemResponse(s.id(), s.name(), s.description(), s.categoryCode(),
						s.categoryName(), s.durationMinutes(), s.priceBase(), s.professionalRequired(),
						s.requiresPriorEvaluation(), s.requiresInformedConsent()))
				.toList();
	}

	@Transactional(readOnly = true)
	public PublicServiceDetailResponse serviceDetail(UUID serviceId) {
		UUID businessId = findDefaultBusiness().getId();
		var s = aestheticRepository.findService(businessId, serviceId);
		return new PublicServiceDetailResponse(s.id(), s.code(), s.name(), s.description(), s.categoryCode(),
				s.categoryName(), s.durationMinutes(), s.priceBase(), s.professionalRequired(), s.supplies(),
				s.contraindications(), s.availabilityRules(), s.bookingRules(), s.cancellationRules(),
				s.aftercareRecommendations(), s.requiresPriorEvaluation(), s.requiresInformedConsent(), s.active());
	}

	@Transactional(readOnly = true)
	public List<PublicServiceBranchResponse> serviceBranches(UUID serviceId) {
		return serviceBranches(serviceId, null, null, null);
	}

	@Transactional(readOnly = true)
	public List<PublicServiceBranchResponse> serviceBranches(UUID serviceId, UUID preferredLocationId, Double latitude,
			Double longitude) {
		UUID businessId = findDefaultBusiness().getId();
		return aestheticRepository.findServiceBranches(businessId, serviceId).stream()
				.sorted(serviceBranchComparator(preferredLocationId, latitude, longitude))
				.map(b -> new PublicServiceBranchResponse(b.id(), b.name(), b.address(), b.commune(), b.phone(),
						b.professionalCount(), b.latitude(), b.longitude(), b.dailyBookingCapacity(),
						distanceKm(latitude, longitude, b.latitude(), b.longitude()),
						b.id().equals(preferredLocationId)))
				.toList();
	}

	@Transactional(readOnly = true)
	public WhatsAppEntryResponse getWhatsAppEntry() {
		BusinessEntity business = findDefaultBusiness();
		UUID businessId = business.getId();

		BusinessLocationRecord location = locationRepository.findDefaultActive(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_ACTIVE_LOCATION",
						"No hay una sede activa configurada."));

		ChannelAccountRecord channel = metaOnboardingRepository.findCloudApiChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_NOT_CONFIGURED",
						"WhatsApp Cloud API no esta configurado para este negocio."));

		String phone = firstNonBlank(channel.phoneNumber(), channel.normalizedPhoneNumber(),
				channel.displayPhoneNumber());
		if (phone == null || phone.isBlank()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "NO_PHONE_NUMBER",
					"El canal de WhatsApp no tiene un numero telefonico configurado.");
		}
		String cleanPhone = phone.replaceAll("\\D", "");
		String prefix = location.code() != null ? "SEDE:" + location.code() + " " : "";
		String message = prefix + "Quiero realizar una reserva";
		String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
		String waUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;

		return new WhatsAppEntryResponse(waUrl, cleanPhone,
				channel.displayPhoneNumber() != null ? channel.displayPhoneNumber() : cleanPhone, message,
				location.code() != null ? location.code() : "", location.name());
	}

	private BusinessEntity findDefaultBusiness() {
		return metaOnboardingRepository.findCentralizedChannel()
				.map(channel -> businessRepository.findById(channel.businessId())
						.orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_BUSINESS_FOR_CHANNEL",
								"El canal centralizado no tiene un negocio valido.", java.util.Map.of())))
				.orElseGet(() -> businessRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
						.orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_BUSINESS_CONFIGURED",
								"No hay un negocio configurado en el sistema.", java.util.Map.of())));
	}

	private AgendaAvailabilityResponse emptyAvailability(LocationRecord location,
			CompleteAgendaJdbcRepository.ServiceRecord service, LocalDate date) {
		return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), date,
				service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
	}

	private void collectSlots(UUID businessId, LocationRecord location,
			CompleteAgendaJdbcRepository.ServiceRecord service, ProfessionalRecord professional, RoomRecord room,
			AgendaAvailabilityRequest request, List<TimeWindowRecord> businessHours,
			List<TimeWindowRecord> professionalHours, List<AgendaSlotResponse> slots, int limit) {
		ZoneId locationZone = resolveLocationZone(location);
		int slotStepMinutes = bookingPolicyService.getSlotStepMinutes(businessId, location.id());
		OffsetDateTime nowAtLocation = OffsetDateTime.now(locationZone);
		for (TimeWindowRecord businessWindow : businessHours) {
			for (TimeWindowRecord profWindow : professionalHours) {
				LocalTime start = max(businessWindow.startTime(), profWindow.startTime());
				LocalTime end = min(businessWindow.endTime(), profWindow.endTime());
				if (!end.isAfter(start))
					continue;
				LocalTime cursor = start;
				while (!cursor.plusMinutes(service.durationMinutes()).isAfter(end) && slots.size() < limit) {
					OffsetDateTime slotStart = request.date().atTime(cursor).atZone(locationZone).toOffsetDateTime();
					OffsetDateTime effectiveStart = slotStart.minusMinutes(service.preparationMinutes());
					OffsetDateTime endsAt = slotStart.plusMinutes(service.durationMinutes())
							.plusMinutes(service.cleanupMinutes());
					String rejectionReason = null;
					if (!slotStart.isAfter(nowAtLocation)) {
						rejectionReason = "PAST";
					} else if (agendaRepository.hasConflict(businessId, null, location.id(), professional.id(),
							room.id(), effectiveStart, endsAt)) {
						rejectionReason = "CONFLICT";
					} else if (agendaRepository.hasBlock(businessId, location.id(), professional.id(), room.id(),
							effectiveStart, endsAt)) {
						rejectionReason = "BLOCKED";
					}
					if (rejectionReason == null) {
						slots.add(new AgendaSlotResponse(slotStart, endsAt, location.id(), location.name(),
								service.id(), service.name(), service.durationMinutes(), professional.id(),
								professional.name(), room.id(), room.name(), true, "Disponible"));
					} else {
						agendaRepository.recordSlotDiscard(businessId, location.id(), service.id(), professional.id(),
								room.id(), slotStart, slotStart.plusMinutes(service.durationMinutes()), effectiveStart,
								endsAt, rejectionReason, "PUBLIC_LANDING");
					}
					cursor = cursor.plusMinutes(slotStepMinutes);
				}
			}
		}
	}

	private ZoneId resolveLocationZone(LocationRecord location) {
		String timezone = location.timezone();
		if (timezone == null || timezone.isBlank())
			return ZoneId.of("America/Santiago");
		try {
			return ZoneId.of(timezone.trim());
		} catch (DateTimeException e) {
			return ZoneId.of("America/Santiago");
		}
	}

	private LocalTime max(LocalTime a, LocalTime b) {
		return a.isAfter(b) ? a : b;
	}
	private LocalTime min(LocalTime a, LocalTime b) {
		return a.isBefore(b) ? a : b;
	}

	private int normalizeLimit(Integer maxSlots) {
		if (maxSlots == null)
			return 12;
		return Math.min(Math.max(maxSlots, 1), 40);
	}

	private String generateConfirmationLink(UUID businessId, UUID bookingId, boolean requiresDeposit) {
		var booking = bookingConfirmationRepository.findBooking(businessId, bookingId);
		BookingStateMachine.assertCanReceiveConfirmationLink(booking.bookingStatus());

		int expirationMinutes = bookingConfirmationProperties.getExpirationMinutes();
		bookingConfirmationRepository.invalidateActiveLinks(businessId, bookingId);

		String token = generateConfirmationToken();
		String confirmationUrl = buildConfirmationUrl(token);
		String tokenHash = tokenHashService.sha256(token);
		OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes);

		UUID linkId = bookingConfirmationRepository.insertLink(businessId, bookingId, tokenHash, confirmationUrl,
				expiresAt);

		String targetStatus = requiresDeposit
				? BookingStateMachine.PENDING_PAYMENT
				: BookingStateMachine.PENDING_CONFIRMATION;
		BookingStateMachine.assertTransition(booking.bookingStatus(), targetStatus, "quedar pendiente de confirmacion");
		bookingConfirmationRepository.updateBookingStatus(businessId, bookingId, targetStatus);
		if (!targetStatus.equals(BookingStateMachine.canonical(booking.bookingStatus()))) {
			agendaRepository.insertStatusHistory(businessId, bookingId, booking.bookingStatus(), targetStatus,
					"Reserva marcada como pendiente de confirmacion desde landing publica.", null, "PUBLIC_LINK");
		}

		auditService.record(businessId, null, "BOOKING_CONFIRMATION_LINK_CREATED", "BOOKING", bookingId,
				"Enlace de confirmacion generado desde landing publica.",
				AuditMetadata.of("linkId", linkId, "expiresAt", expiresAt));

		return confirmationUrl;
	}

	private void sendWhatsAppConfirmationLink(UUID businessId, UUID bookingId, String locationName, String serviceName,
			String professionalName, OffsetDateTime startsAt, String customerName, String customerPhone,
			int durationMinutes, String confirmationUrl) {
		int expirationMinutes = bookingConfirmationProperties.getExpirationMinutes();
		OffsetDateTime sentAt = null;

		var linkRecord = bookingConfirmationRepository.findLatestByBooking(businessId, bookingId);
		if (linkRecord.isEmpty())
			return;
		var link = linkRecord.get();
		UUID linkId = link.linkId();
		OffsetDateTime expiresAt = link.expiresAt();

		try {
			String body = WhatsAppMessageFormatter.temporaryBookingCreated(serviceName, locationName,
					startsAt.toLocalDate().toString(), startsAt.toLocalTime().toString(), confirmationUrl,
					expirationMinutes);

			ChannelDispatchResponse response = channelDispatchService
					.dispatch(new ChannelDispatchRequest(businessId, MessageChannelType.WHATSAPP, customerPhone, body));
			sentAt = OffsetDateTime.now(ZoneOffset.UTC);
			bookingConfirmationRepository.markSent(linkId, sentAt);

			auditService.record(businessId, null, "BOOKING_CONFIRMATION_WHATSAPP_SENT", "BOOKING", bookingId,
					"Enlace de confirmacion enviado por WhatsApp desde landing publica.",
					AuditMetadata.of("linkId", linkId, "expiresAt", expiresAt, "channelStatus", response.status()));
		} catch (RuntimeException e) {
			auditService.record(businessId, null, "BOOKING_CONFIRMATION_WHATSAPP_SEND_FAILED", "BOOKING", bookingId,
					"Fallo envio de WhatsApp desde landing: "
							+ (e.getMessage() != null ? e.getMessage() : "sin detalle"),
					AuditMetadata.of("linkId", linkId, "expiresAt", expiresAt, "error", e.getMessage()));
		}
	}

	private String generateConfirmationToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String buildConfirmationUrl(String token) {
		String baseUrl = bookingConfirmationProperties.getPublicBaseUrl();
		return baseUrl.endsWith("/") ? baseUrl + token : baseUrl + "/" + token;
	}

	private void validateConsentRequirements(CompleteAgendaJdbcRepository.ServiceRecord service,
			Boolean informedConsentAccepted, LocalDate customerBirthDate, String guardianName, String guardianPhone,
			OffsetDateTime startsAt, LocationRecord location) {
		if (service.requiresInformedConsent() && !Boolean.TRUE.equals(informedConsentAccepted)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PUBLIC_BOOKING_VALIDATION_ERROR",
					"La solicitud de reserva contiene datos invalidos.", java.util.Map.of("informedConsentAccepted",
							"Debes aceptar el consentimiento informado para este servicio."));
		}
		if (customerBirthDate == null) {
			return;
		}
		LocalDate bookingDate = startsAt.atZoneSameInstant(BookingValidationService.resolveLocationZone(location))
				.toLocalDate();
		if (customerBirthDate.isAfter(bookingDate)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PUBLIC_BOOKING_VALIDATION_ERROR",
					"La solicitud de reserva contiene datos invalidos.",
					java.util.Map.of("customerBirthDate", "La fecha de nacimiento no puede ser futura."));
		}
		if (Period.between(customerBirthDate, bookingDate).getYears() < 18
				&& (isBlank(guardianName) || isBlank(guardianPhone))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PUBLIC_BOOKING_VALIDATION_ERROR",
					"La solicitud de reserva contiene datos invalidos.", java.util.Map.of("guardianName",
							"Debes indicar nombre y telefono del tutor para menores de edad."));
		}
	}

	private void recordConsentIfPresent(UUID businessId, UUID bookingId,
			CompleteAgendaJdbcRepository.ServiceRecord service, Boolean informedConsentAccepted,
			LocalDate customerBirthDate, String guardianName, String guardianPhone) {
		if (!service.requiresInformedConsent() && informedConsentAccepted == null && customerBirthDate == null
				&& isBlank(guardianName) && isBlank(guardianPhone)) {
			return;
		}
		agendaRepository.recordBookingConsent(businessId, bookingId, service.requiresInformedConsent(),
				Boolean.TRUE.equals(informedConsentAccepted), customerBirthDate, guardianName, guardianPhone);
	}

	private UUID resolveIdempotentPublicBooking(UUID businessId, String idempotencyKey, String requestHash) {
		CompleteAgendaJdbcRepository.BookingOperationIdempotencyRecord existing = agendaRepository
				.findBookingOperationIdempotency(businessId, "PUBLIC_BOOKING_CREATE", idempotencyKey);
		if (existing == null) {
			boolean reserved = agendaRepository.reserveBookingOperationIdempotency(businessId, "PUBLIC_BOOKING_CREATE",
					idempotencyKey, requestHash, "LANDING");
			if (reserved) {
				return null;
			}
			existing = agendaRepository.findBookingOperationIdempotency(businessId, "PUBLIC_BOOKING_CREATE",
					idempotencyKey);
		}
		if (existing == null) {
			throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENT_OPERATION_IN_PROGRESS",
					"La operacion de reserva ya esta en proceso.", java.util.Map.of("idempotencyKey", idempotencyKey));
		}
		if (!requestHash.equals(existing.requestHash())) {
			throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
					"La clave de idempotencia ya fue usada con otra solicitud.",
					java.util.Map.of("idempotencyKey", idempotencyKey));
		}
		if (existing.bookingId() != null && "COMPLETED".equals(existing.status())) {
			return existing.bookingId();
		}
		throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENT_OPERATION_IN_PROGRESS",
				"La operacion de reserva ya esta en proceso.", java.util.Map.of("idempotencyKey", idempotencyKey));
	}

	private String bookingOperationIdempotencyKey(String operationType, String explicitKey, UUID businessId,
			UUID conversationId, UUID customerId, String customerPhone, UUID locationId, UUID serviceId,
			UUID professionalId, UUID roomId, OffsetDateTime startsAt) {
		if (explicitKey != null && !explicitKey.isBlank()) {
			return explicitKey.trim();
		}
		return stableUuid(operationType, businessId, conversationId, customerId, customerPhone, locationId, serviceId,
				professionalId, roomId, startsAt);
	}

	private String bookingOperationRequestHash(String operationType, UUID businessId, UUID conversationId,
			UUID customerId, String customerPhone, UUID locationId, UUID serviceId, UUID professionalId, UUID roomId,
			OffsetDateTime startsAt, int durationMinutes) {
		return stableUuid(operationType, businessId, conversationId, customerId, customerPhone, locationId, serviceId,
				professionalId, roomId, startsAt, durationMinutes);
	}

	private String stableUuid(Object... values) {
		StringBuilder builder = new StringBuilder();
		for (Object value : values) {
			builder.append(value == null ? "" : value).append('|');
		}
		return UUID.nameUUIDFromBytes(builder.toString().getBytes(StandardCharsets.UTF_8)).toString();
	}

	private Comparator<BusinessLocationRecord> locationComparator(UUID preferredLocationId, Double latitude,
			Double longitude) {
		return Comparator.comparing((BusinessLocationRecord l) -> !l.id().equals(preferredLocationId))
				.thenComparing(l -> distanceSortValue(latitude, longitude, l.latitude(), l.longitude()))
				.thenComparing(l -> l.dailyBookingCapacity() == null ? Integer.MAX_VALUE : -l.dailyBookingCapacity())
				.thenComparing(BusinessLocationRecord::name);
	}

	private Comparator<AestheticCenterJdbcRepository.ServiceBranchRecord> serviceBranchComparator(
			UUID preferredLocationId, Double latitude, Double longitude) {
		return Comparator
				.comparing((AestheticCenterJdbcRepository.ServiceBranchRecord b) -> !b.id().equals(preferredLocationId))
				.thenComparing(b -> distanceSortValue(latitude, longitude, b.latitude(), b.longitude()))
				.thenComparing(b -> b.dailyBookingCapacity() == null ? Integer.MAX_VALUE : -b.dailyBookingCapacity())
				.thenComparing(AestheticCenterJdbcRepository.ServiceBranchRecord::name);
	}

	private double distanceSortValue(Double latitude, Double longitude, BigDecimal locationLatitude,
			BigDecimal locationLongitude) {
		Double distance = distanceKm(latitude, longitude, locationLatitude, locationLongitude);
		return distance == null ? Double.MAX_VALUE : distance;
	}

	private Double distanceKm(Double latitude, Double longitude, BigDecimal locationLatitude,
			BigDecimal locationLongitude) {
		if (latitude == null || longitude == null || locationLatitude == null || locationLongitude == null) {
			return null;
		}
		double earthRadiusKm = 6371.0;
		double lat1 = Math.toRadians(latitude);
		double lat2 = Math.toRadians(locationLatitude.doubleValue());
		double deltaLat = Math.toRadians(locationLatitude.doubleValue() - latitude);
		double deltaLon = Math.toRadians(locationLongitude.doubleValue() - longitude);
		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return Math.round(earthRadiusKm * c * 100.0) / 100.0;
	}

	private String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PUBLIC_BOOKING_VALIDATION_ERROR",
					"La solicitud de reserva contiene datos invalidos.",
					java.util.Map.of("customerPhone", "Ingresa un telefono valido."));
		}
		return phone.replace(" ", "").trim();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
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
}
