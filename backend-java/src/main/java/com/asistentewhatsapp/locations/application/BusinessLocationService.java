package com.asistentewhatsapp.locations.application;

import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.locations.api.BusinessLocationResponse;
import com.asistentewhatsapp.locations.api.UpsertBusinessLocationRequest;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.ConflictException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessLocationService {

	private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
	private final MetaOnboardingRepository metaOnboardingRepository;

	public BusinessLocationService(BusinessLocationJdbcRepository businessLocationJdbcRepository,
			MetaOnboardingRepository metaOnboardingRepository) {
		this.businessLocationJdbcRepository = businessLocationJdbcRepository;
		this.metaOnboardingRepository = metaOnboardingRepository;
	}

	@Transactional(readOnly = true)
	public List<BusinessLocationResponse> list(AuthenticatedUser authenticatedUser, boolean activeOnly) {
		List<BusinessLocationRecord> locations = activeOnly
				? businessLocationJdbcRepository.findActive(authenticatedUser.businessId())
				: businessLocationJdbcRepository.findAll(authenticatedUser.businessId());
		return locations.stream().map(businessLocationJdbcRepository::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public BusinessLocationResponse getDetail(AuthenticatedUser authenticatedUser, UUID locationId) {
		return businessLocationJdbcRepository
				.toResponse(businessLocationJdbcRepository.findById(authenticatedUser.businessId(), locationId));
	}

	@Transactional
	public BusinessLocationResponse create(AuthenticatedUser authenticatedUser, UpsertBusinessLocationRequest request) {
		NormalizedLocation normalized = normalize(request, authenticatedUser.timezone(), true);
		ensureCodeAvailable(authenticatedUser.businessId(), normalized.code(), null);
		UUID locationId = businessLocationJdbcRepository.insert(authenticatedUser.businessId(), normalized.code(),
				normalized.name(), normalized.address(), normalized.city(), normalized.commune(), normalized.phone(),
				normalized.whatsappNumber(), normalized.timezone(), normalized.active());
		return getDetail(authenticatedUser, locationId);
	}

	@Transactional
	public BusinessLocationResponse update(AuthenticatedUser authenticatedUser, UUID locationId,
			UpsertBusinessLocationRequest request) {
		BusinessLocationRecord current = businessLocationJdbcRepository.findById(authenticatedUser.businessId(),
				locationId);
		NormalizedLocation normalized = normalize(request, current.timezone(), current.active());
		ensureCodeAvailable(authenticatedUser.businessId(), normalized.code(), locationId);
		if (current.active() && !normalized.active()
				&& businessLocationJdbcRepository.countActive(authenticatedUser.businessId()) <= 1) {
			throw validationError("active", "No se puede desactivar la ultima sede activa del negocio.");
		}
		businessLocationJdbcRepository.update(authenticatedUser.businessId(), locationId, normalized.code(),
				normalized.name(), normalized.address(), normalized.city(), normalized.commune(), normalized.phone(),
				normalized.whatsappNumber(), normalized.timezone(), normalized.active());
		return getDetail(authenticatedUser, locationId);
	}

	@Transactional
	public BusinessLocationResponse deactivate(AuthenticatedUser authenticatedUser, UUID locationId) {
		BusinessLocationRecord current = businessLocationJdbcRepository.findById(authenticatedUser.businessId(),
				locationId);
		if (!current.active()) {
			return businessLocationJdbcRepository.toResponse(current);
		}
		if (businessLocationJdbcRepository.countActive(authenticatedUser.businessId()) <= 1) {
			throw validationError("active", "No se puede desactivar la ultima sede activa del negocio.");
		}
		businessLocationJdbcRepository.deactivate(authenticatedUser.businessId(), locationId);
		return getDetail(authenticatedUser, locationId);
	}

	@Transactional(readOnly = true)
	public Map<String, String> commercialQr(AuthenticatedUser authenticatedUser, UUID locationId) {
		BusinessLocationRecord location = businessLocationJdbcRepository.findById(authenticatedUser.businessId(),
				locationId);

		ChannelAccountRecord channel = metaOnboardingRepository.findCloudApiChannel(authenticatedUser.businessId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_NOT_CONFIGURED",
						"WhatsApp Cloud API no esta configurado para este negocio."));

		String phone = channel.phoneNumber();
		if (phone == null || phone.isBlank()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "NO_PHONE_NUMBER",
					"El canal de WhatsApp no tiene un numero telefonico configurado.");
		}
		String cleanPhone = phone.replaceAll("\\D", "");
		String prefix = location.code() != null ? "SEDE:" + location.code() + " " : "";
		String message = prefix + "Quiero realizar una reserva";
		String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
		String waUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;

		String displayPhone = channel.displayPhoneNumber() != null ? channel.displayPhoneNumber() : cleanPhone;

		return Map.of("waUrl", waUrl, "phoneNumber", cleanPhone, "displayPhoneNumber", displayPhone, "prefilledMessage",
				message, "locationCode", location.code() != null ? location.code() : "", "locationName",
				location.name() != null ? location.name() : "");
	}

	private void ensureCodeAvailable(UUID businessId, String code, UUID excludedLocationId) {
		if (businessLocationJdbcRepository.existsByCode(businessId, code, excludedLocationId)) {
			throw new ConflictException("Ya existe una sede con el codigo indicado.",
					Map.of("code", "El codigo de sede ya existe para este negocio."));
		}
	}

	private NormalizedLocation normalize(UpsertBusinessLocationRequest request, String defaultTimezone,
			boolean defaultActive) {
		String code = normalizeRequired(request.code(), "code", 50).toLowerCase();
		if (!code.matches("[a-z0-9][a-z0-9_-]{1,49}")) {
			throw validationError("code", "El codigo debe usar letras, numeros, guion o guion bajo.");
		}
		return new NormalizedLocation(code, normalizeRequired(request.name(), "name", 150),
				normalizeOptional(request.address(), "address", 255), normalizeOptional(request.city(), "city", 120),
				normalizeOptional(request.commune(), "commune", 120), normalizeOptional(request.phone(), "phone", 30),
				normalizeOptional(request.whatsappNumber(), "whatsappNumber", 30),
				normalizeOptional(request.timezone(), "timezone", 60) != null
						? normalizeOptional(request.timezone(), "timezone", 60)
						: normalizeRequired(defaultTimezone, "timezone", 60),
				request.active() == null ? defaultActive : request.active());
	}

	private String normalizeRequired(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw validationError(field, "Este campo es obligatorio.");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw validationError(field, "El valor supera el largo maximo permitido.");
		}
		return normalized;
	}

	private String normalizeOptional(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw validationError(field, "El valor supera el largo maximo permitido.");
		}
		return normalized;
	}

	private ApiException validationError(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos.",
				Map.of(field, message));
	}

	private record NormalizedLocation(String code, String name, String address, String city, String commune,
			String phone, String whatsappNumber, String timezone, boolean active) {
	}
}
