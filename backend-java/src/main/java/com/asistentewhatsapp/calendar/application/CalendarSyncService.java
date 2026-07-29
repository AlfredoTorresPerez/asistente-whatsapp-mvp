package com.asistentewhatsapp.calendar.application;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.GoogleCalendarApiException;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.RefreshResult;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarSyncService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncService.class);
	private static final String TIMEZONE = "America/Santiago";

	private final BookingCalendarSyncJdbcRepository syncRepository;
	private final CalendarIntegrationJdbcRepository accountRepository;
	private final TokenEncryptionService tokenEncryption;
	private final Map<String, CalendarProvider> providers;
	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final int maxRetries;
	private final long retryIntervalMs;

	public CalendarSyncService(BookingCalendarSyncJdbcRepository syncRepository,
			CalendarIntegrationJdbcRepository accountRepository, TokenEncryptionService tokenEncryption,
			List<CalendarProvider> providerList, NamedParameterJdbcTemplate jdbcTemplate,
			@Value("${app.calendar.sync.max-retries:5}") int maxRetries,
			@Value("${app.calendar.sync.retry-interval-ms:300000}") long retryIntervalMs) {
		this.syncRepository = syncRepository;
		this.accountRepository = accountRepository;
		this.tokenEncryption = tokenEncryption;
		this.jdbcTemplate = jdbcTemplate;
		this.maxRetries = maxRetries;
		this.retryIntervalMs = retryIntervalMs;
		Map<String, CalendarProvider> map = new ConcurrentHashMap<>();
		for (CalendarProvider p : providerList) {
			map.put(p.getProviderName(), p);
		}
		this.providers = map;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void syncConfirmed(UUID bookingId, UUID businessId) {
		CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
		scheduleSync(bookingId, businessId, "CREATE", eventData);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void syncRescheduled(UUID bookingId, UUID businessId) {
		CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
		scheduleSync(bookingId, businessId, "UPDATE", eventData);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void syncCancelled(UUID bookingId, UUID businessId) {
		CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
		scheduleSync(bookingId, businessId, "DELETE", eventData);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void scheduleSync(UUID bookingId, UUID businessId, String syncAction, CalendarEventData eventData) {
		List<CalendarIntegrationAccountRecord> accounts = accountRepository.findActiveByBusiness(businessId);
		if (accounts.isEmpty()) {
			LOGGER.debug("CALENDAR_NO_ACCOUNTS bookingId={} businessId={}", bookingId, businessId);
			return;
		}
		for (CalendarIntegrationAccountRecord account : accounts) {
			CalendarProvider provider = providers.get(account.provider());
			if (provider == null) {
				LOGGER.warn("CALENDAR_UNKNOWN_PROVIDER provider={}", account.provider());
				continue;
			}
			Optional<BookingCalendarSyncRecord> existing = syncRepository.findByBookingAndProviderOptional(bookingId,
					account.provider());
			if (existing.isPresent()) {
				BookingCalendarSyncRecord rec = existing.get();
				String currentStatus = rec.syncStatus();
				String currentEventId = rec.externalEventId();

				if ("SYNCED".equals(currentStatus) && "DELETE".equals(syncAction)) {
					performSync(rec, provider, account, eventData);
				} else if ("SYNCED".equals(currentStatus) && "UPDATE".equals(syncAction)) {
					if (currentEventId != null) {
						BookingCalendarSyncRecord updated = new BookingCalendarSyncRecord(rec.id(), rec.bookingId(),
								rec.businessId(), rec.provider(), currentEventId, "PENDING", "UPDATE", null, 0, null,
								rec.lastSuccessfulSyncAt(), rec.createdAt(), OffsetDateTime.now());
						syncRepository.insert(updated);
					}
				} else if ("SYNCED".equals(currentStatus) && "CREATE".equals(syncAction)) {
					LOGGER.debug("CALENDAR_SYNC_ALREADY_SYNCED bookingId={} provider={} eventId={}", bookingId,
							account.provider(), currentEventId);
				} else if ("FAILED".equals(currentStatus)) {
					BookingCalendarSyncRecord updated = new BookingCalendarSyncRecord(rec.id(), rec.bookingId(),
							rec.businessId(), rec.provider(), currentEventId, "PENDING", syncAction, null,
							rec.retryCount(), rec.lastSyncAttemptAt(), rec.lastSuccessfulSyncAt(), rec.createdAt(),
							OffsetDateTime.now());
					syncRepository.insert(updated);
				} else if ("PENDING".equals(currentStatus)) {
					LOGGER.debug("CALENDAR_SYNC_ALREADY_PENDING bookingId={} provider={}", bookingId,
							account.provider());
				} else if ("CANCELLED".equals(currentStatus)) {
					BookingCalendarSyncRecord updated = new BookingCalendarSyncRecord(rec.id(), rec.bookingId(),
							rec.businessId(), rec.provider(), currentEventId, "PENDING", syncAction, null, 0, null,
							null, rec.createdAt(), OffsetDateTime.now());
					syncRepository.insert(updated);
				} else {
					LOGGER.debug("CALENDAR_SYNC_SKIP bookingId={} provider={} status={}", bookingId, account.provider(),
							currentStatus);
				}
			} else {
				BookingCalendarSyncRecord record = new BookingCalendarSyncRecord(UUID.randomUUID(), bookingId,
						businessId, account.provider(), null, "PENDING", syncAction, null, 0, null, null,
						OffsetDateTime.now(), OffsetDateTime.now());
				syncRepository.insert(record);
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void performSync(BookingCalendarSyncRecord record, CalendarProvider provider,
			CalendarIntegrationAccountRecord account, CalendarEventData eventData) {
		if (eventData == null) {
			syncRepository.updateSyncFailed(record.id(), "No hay datos del evento para sincronizar.");
			return;
		}
		try {
			String accessToken = tokenEncryption.decrypt(account.accessTokenEncrypted());
			if (accessToken == null) {
				syncRepository.updateSyncFailed(record.id(), "No hay token de acceso disponible");
				markRequiresReconnect(account);
				return;
			}
			boolean tokenRefreshed = false;
			if (isTokenExpired(account) && account.refreshTokenEncrypted() != null) {
				String refreshToken = tokenEncryption.decrypt(account.refreshTokenEncrypted());
				if (refreshToken != null) {
					try {
						RefreshResult refreshResult = provider.refreshAccessToken(refreshToken);
						String newEncryptedAccess = tokenEncryption.encrypt(refreshResult.accessToken());
						OffsetDateTime newExpiry = refreshResult.expiresInSeconds() != null
								? OffsetDateTime.now().plusSeconds(refreshResult.expiresInSeconds())
								: null;
						accountRepository.updateTokens(account.id(), account.businessId(), newEncryptedAccess,
								account.refreshTokenEncrypted(), newExpiry);
						accessToken = refreshResult.accessToken();
						tokenRefreshed = true;
						LOGGER.info("CALENDAR_TOKEN_REFRESHED accountId={}", account.id());
					} catch (GoogleCalendarApiException e) {
						LOGGER.warn("CALENDAR_TOKEN_REFRESH_FAILED accountId={} reason={}", account.id(),
								e.getMessage());
						handleSyncError(record, account, e);
						return;
					} catch (Exception e) {
						LOGGER.warn("CALENDAR_TOKEN_REFRESH_FAILED accountId={} reason={}", account.id(),
								e.getMessage());
						syncRepository.updateSyncFailed(record.id(), "Token refresh failed: " + e.getMessage());
						return;
					}
				}
			}

			String externalEventId = record.externalEventId();
			String action = record.syncAction();
			String calendarId = account.calendarId();

			if (calendarId == null) {
				syncRepository.updateSyncFailed(record.id(), "No hay calendario seleccionado");
				return;
			}

			try {
				if ("DELETE".equals(action)) {
					if (externalEventId != null) {
						try {
							provider.deleteEvent(calendarId, externalEventId, accessToken);
						} catch (GoogleCalendarApiException e) {
							if (!e.isNotFound()) {
								throw e;
							}
							LOGGER.info("CALENDAR_SYNC_EVENT_ALREADY_DELETED eventId={}", externalEventId);
						}
					}
					syncRepository.updateStatus(record.id(), "CANCELLED", externalEventId, null);
					LOGGER.info("CALENDAR_SYNC_CANCELLED syncId={} provider={} eventId={}", record.id(),
							record.provider(), externalEventId);

				} else if ("UPDATE".equals(action)) {
					if (externalEventId != null) {
						try {
							provider.updateEvent(calendarId, externalEventId, eventData, accessToken);
						} catch (GoogleCalendarApiException e) {
							if (e.isNotFound()) {
								LOGGER.info("CALENDAR_SYNC_EVENT_NOT_FOUND recreating eventId={}", externalEventId);
								String newEventId = computeIdempotentId(eventData.businessId(), eventData.bookingId());
								CalendarEventData updatedEvent = new CalendarEventData(eventData.summary(),
										eventData.description(), eventData.location(), eventData.startAt(),
										eventData.endAt(), eventData.timezone(), eventData.attendeeEmail(),
										eventData.attendeeName(), eventData.businessId(), eventData.bookingId(),
										newEventId);
								externalEventId = provider.createEvent(calendarId, updatedEvent, accessToken);
							} else {
								throw e;
							}
						}
					} else {
						externalEventId = computeIdempotentId(eventData.businessId(), eventData.bookingId());
						CalendarEventData updatedEvent = new CalendarEventData(eventData.summary(),
								eventData.description(), eventData.location(), eventData.startAt(), eventData.endAt(),
								eventData.timezone(), eventData.attendeeEmail(), eventData.attendeeName(),
								eventData.businessId(), eventData.bookingId(), externalEventId);
						externalEventId = provider.createEvent(calendarId, updatedEvent, accessToken);
					}
					syncRepository.updateSyncSuccess(record.id(), externalEventId);
					accountRepository.updateLastSyncAt(account.id(), account.businessId(), OffsetDateTime.now());
					LOGGER.info("CALENDAR_SYNC_UPDATED syncId={} provider={} eventId={}", record.id(),
							record.provider(), externalEventId);

				} else if ("CREATE".equals(action)) {
					String idempotentId = computeIdempotentId(eventData.businessId(), eventData.bookingId());
					CalendarEventData newEvent = new CalendarEventData(eventData.summary(), eventData.description(),
							eventData.location(), eventData.startAt(), eventData.endAt(), eventData.timezone(),
							eventData.attendeeEmail(), eventData.attendeeName(), eventData.businessId(),
							eventData.bookingId(), idempotentId);
					try {
						externalEventId = provider.createEvent(calendarId, newEvent, accessToken);
					} catch (GoogleCalendarApiException e) {
						if (e.isConflict()) {
							LOGGER.info("CALENDAR_SYNC_CONFLICT fetching existing event idempotentId={}", idempotentId);
							CalendarEventData existingEvent = provider.getEvent(calendarId, idempotentId, accessToken);
							if (existingEvent != null && existingEvent.googleEventId() != null) {
								externalEventId = existingEvent.googleEventId();
								provider.updateEvent(calendarId, externalEventId, eventData, accessToken);
							} else {
								throw e;
							}
						} else {
							throw e;
						}
					}
					syncRepository.updateSyncSuccess(record.id(), externalEventId);
					accountRepository.updateLastSyncAt(account.id(), account.businessId(), OffsetDateTime.now());
					LOGGER.info("CALENDAR_SYNC_CREATED syncId={} provider={} eventId={}", record.id(),
							record.provider(), externalEventId);
				} else {
					syncRepository.updateSyncFailed(record.id(), "Unknown sync action: " + action);
				}
			} catch (GoogleCalendarApiException e) {
				handleSyncError(record, account, e);
			}
		} catch (GoogleCalendarApiException e) {
			handleSyncError(record, account, e);
		} catch (Exception e) {
			String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			LOGGER.warn("CALENDAR_SYNC_FAILED syncId={} provider={} reason={}", record.id(), record.provider(), msg);
			syncRepository.updateSyncFailed(record.id(), msg);
		}
	}

	private void handleSyncError(BookingCalendarSyncRecord record, CalendarIntegrationAccountRecord account,
			GoogleCalendarApiException e) {
		String msg = e.getMessage();
		LOGGER.warn("CALENDAR_SYNC_ERROR syncId={} provider={} statusCode={} reason={}", record.id(), record.provider(),
				e.getStatusCode(), msg);

		if (e.isAuthError()) {
			if (hasRefreshToken(account)) {
				try {
					String refreshToken = tokenEncryption.decrypt(account.refreshTokenEncrypted());
					CalendarProvider provider = getProvider(account.provider());
					RefreshResult refreshResult = provider.refreshAccessToken(refreshToken);
					String newEncryptedAccess = tokenEncryption.encrypt(refreshResult.accessToken());
					OffsetDateTime newExpiry = refreshResult.expiresInSeconds() != null
							? OffsetDateTime.now().plusSeconds(refreshResult.expiresInSeconds())
							: null;
					accountRepository.updateTokens(account.id(), account.businessId(), newEncryptedAccess,
							account.refreshTokenEncrypted(), newExpiry);
					String newAccessToken = refreshResult.accessToken();
					account = accountRepository.findByIdAndBusiness(account.id(), account.businessId()).orElse(account);
					retryWithNewToken(record, provider, account, newAccessToken, e);
					return;
				} catch (Exception refreshError) {
					LOGGER.warn("CALENDAR_TOKEN_REFRESH_FAILED accountId={} reason={}", account.id(),
							refreshError.getMessage());
				}
			}
			syncRepository.updateSyncFailed(record.id(), "Auth error: " + msg);
			if (e.isPermanentError()) {
				markRequiresReconnect(account);
			}
		} else if (e.isRateLimit() || e.isServerError()) {
			syncRepository.updateSyncFailed(record.id(), "Retryable error: " + msg);
		} else if (e.isNotFound()) {
			syncRepository.updateSyncFailed(record.id(), "Not found: " + msg);
		} else if (e.isConflict()) {
			syncRepository.updateSyncFailed(record.id(), "Conflict: " + msg);
		} else {
			syncRepository.updateSyncFailed(record.id(), msg);
			if (e.isPermanentError()) {
				markRequiresReconnect(account);
			}
		}
	}

	private void retryWithNewToken(BookingCalendarSyncRecord record, CalendarProvider provider,
			CalendarIntegrationAccountRecord account, String newAccessToken, GoogleCalendarApiException originalError) {
		try {
			String externalEventId = record.externalEventId();
			String action = record.syncAction();
			String calendarId = account.calendarId();

			if ("DELETE".equals(action) && externalEventId != null) {
				provider.deleteEvent(calendarId, externalEventId, newAccessToken);
				syncRepository.updateStatus(record.id(), "CANCELLED", externalEventId, null);
				LOGGER.info("CALENDAR_SYNC_CANCELLED_AFTER_REFRESH syncId={}", record.id());
			} else if ("UPDATE".equals(action) && externalEventId != null) {
				provider.updateEvent(calendarId, externalEventId,
						loadBookingEventData(record.bookingId(), record.businessId()), newAccessToken);
				syncRepository.updateSyncSuccess(record.id(), externalEventId);
				LOGGER.info("CALENDAR_SYNC_UPDATED_AFTER_REFRESH syncId={}", record.id());
			} else if ("CREATE".equals(action)) {
				CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
				String idempotentId = computeIdempotentId(eventData.businessId(), eventData.bookingId());
				String newEventId = provider.createEvent(calendarId, eventData, newAccessToken);
				syncRepository.updateSyncSuccess(record.id(), newEventId);
				LOGGER.info("CALENDAR_SYNC_CREATED_AFTER_REFRESH syncId={}", record.id());
			} else {
				syncRepository.updateSyncFailed(record.id(), "Unknown action after token refresh");
			}
			accountRepository.updateLastSyncAt(account.id(), account.businessId(), OffsetDateTime.now());
		} catch (Exception retryError) {
			String retryMsg = retryError.getMessage() != null
					? retryError.getMessage()
					: retryError.getClass().getSimpleName();
			LOGGER.warn("CALENDAR_RETRY_AFTER_REFRESH_FAILED syncId={} reason={}", record.id(), retryMsg);
			syncRepository.updateSyncFailed(record.id(), "Retry after refresh failed: " + retryMsg);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void retrySync(UUID bookingId, UUID businessId) {
		List<BookingCalendarSyncRecord> records = syncRepository.findByBookingAndBusiness(bookingId, businessId);
		for (BookingCalendarSyncRecord record : records) {
			if (!"FAILED".equals(record.syncStatus()))
				continue;
			Optional<CalendarIntegrationAccountRecord> accountOpt = accountRepository
					.findByBusinessAndProvider(record.businessId(), record.provider());
			if (accountOpt.isEmpty()) {
				LOGGER.warn("CALENDAR_RETRY_NO_ACCOUNT syncId={}", record.id());
				continue;
			}
			CalendarProvider provider = providers.get(record.provider());
			if (provider == null)
				continue;
			CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
			performSync(record, provider, accountOpt.get(), eventData);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void retryFailedSyncs() {
		OffsetDateTime before = OffsetDateTime.now().minusNanos(retryIntervalMs * 1_000_000);
		List<BookingCalendarSyncRecord> failed = syncRepository.findFailedSyncs(maxRetries, before);
		for (BookingCalendarSyncRecord record : failed) {
			Optional<CalendarIntegrationAccountRecord> accountOpt = accountRepository
					.findByBusinessAndProvider(record.businessId(), record.provider());
			if (accountOpt.isEmpty())
				continue;
			CalendarProvider provider = providers.get(record.provider());
			if (provider == null)
				continue;
			CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
			performSync(record, provider, accountOpt.get(), eventData);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processPendingSyncs() {
		List<BookingCalendarSyncRecord> pending = syncRepository.findPendingSyncs(50);
		for (BookingCalendarSyncRecord record : pending) {
			Optional<CalendarIntegrationAccountRecord> accountOpt = accountRepository
					.findByBusinessAndProvider(record.businessId(), record.provider());
			if (accountOpt.isEmpty()) {
				syncRepository.updateSyncFailed(record.id(), "No active account for provider");
				continue;
			}
			CalendarProvider provider = providers.get(record.provider());
			if (provider == null) {
				syncRepository.updateSyncFailed(record.id(), "Unknown provider: " + record.provider());
				continue;
			}
			CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
			performSync(record, provider, accountOpt.get(), eventData);
		}
	}

	public List<BookingCalendarSyncRecord> getSyncStatus(UUID bookingId, UUID businessId) {
		return syncRepository.findByBookingAndBusiness(bookingId, businessId);
	}

	public boolean hasActiveIntegration(UUID businessId) {
		return !accountRepository.findActiveByBusiness(businessId).isEmpty();
	}

	private CalendarProvider getProvider(String providerName) {
		CalendarProvider provider = providers.get(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Unknown provider: " + providerName);
		}
		return provider;
	}

	private boolean isTokenExpired(CalendarIntegrationAccountRecord account) {
		return account.tokenExpiresAt() != null
				&& account.tokenExpiresAt().isBefore(OffsetDateTime.now().plusMinutes(5));
	}

	private boolean hasRefreshToken(CalendarIntegrationAccountRecord account) {
		return account.refreshTokenEncrypted() != null && !account.refreshTokenEncrypted().isBlank();
	}

	private void markRequiresReconnect(CalendarIntegrationAccountRecord account) {
		try {
			accountRepository.updateRequiresReconnect(account.id(), account.businessId(), true);
			LOGGER.warn("CALENDAR_ACCOUNT_REQUIRES_RECONNECT accountId={} provider={}", account.id(),
					account.provider());
		} catch (Exception e) {
			LOGGER.error("Failed to mark account as requiring reconnect", e);
		}
	}

	private String computeIdempotentId(UUID businessId, UUID bookingId) {
		if (businessId == null || bookingId == null)
			return null;
		String input = businessId.toString() + "|" + bookingId.toString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash).substring(0, 40);
		} catch (Exception e) {
			throw new RuntimeException("Failed to compute idempotent event ID", e);
		}
	}

	public CalendarEventData loadBookingEventData(UUID bookingId, UUID businessId) {
		try {
			return jdbcTemplate.queryForObject("""
					select
					    b.starts_at,
					    b.ends_at,
					    b.subject as service_name,
					    c.display_name as customer_name,
					    c.email as customer_email,
					    bl.address as location_address,
					    bl.name as location_name
					from booking b
					left join customer c on c.id = b.customer_id
					left join business_location bl on bl.id = b.location_id
					where b.id = :bookingId and b.business_id = :businessId
					""", Map.of("bookingId", bookingId, "businessId", businessId), (rs, rowNum) -> {
				String loc = rs.getString("location_address");
				String locName = rs.getString("location_name");
				String fullLocation = locName != null ? (loc != null ? locName + ", " + loc : locName) : loc;
				return new CalendarEventData(rs.getString("service_name"),
						"Reserva: " + rs.getString("service_name") + " | Cliente: " + rs.getString("customer_name"),
						fullLocation, rs.getObject("starts_at", OffsetDateTime.class),
						rs.getObject("ends_at", OffsetDateTime.class), TIMEZONE, rs.getString("customer_email"),
						rs.getString("customer_name"), businessId, bookingId, null);
			});
		} catch (Exception e) {
			LOGGER.warn("CALENDAR_LOAD_BOOKING_FAILED bookingId={} reason={}", bookingId, e.getMessage());
			return null;
		}
	}
}
