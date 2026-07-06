package com.asistentewhatsapp.calendar.application;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncService.class);
    private static final String TIMEZONE = "America/Santiago";
    private static final int MAX_RETRIES = 5;

    private final BookingCalendarSyncJdbcRepository syncRepository;
    private final CalendarIntegrationJdbcRepository accountRepository;
    private final TokenEncryptionService tokenEncryption;
    private final Map<String, CalendarProvider> providers;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CalendarSyncService(
            BookingCalendarSyncJdbcRepository syncRepository,
            CalendarIntegrationJdbcRepository accountRepository,
            TokenEncryptionService tokenEncryption,
            List<CalendarProvider> providerList,
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.syncRepository = syncRepository;
        this.accountRepository = accountRepository;
        this.tokenEncryption = tokenEncryption;
        this.jdbcTemplate = jdbcTemplate;
        Map<String, CalendarProvider> map = new ConcurrentHashMap<>();
        for (CalendarProvider p : providerList) {
            map.put(p.getProviderName(), p);
        }
        this.providers = map;
    }

    public void syncConfirmed(UUID bookingId, UUID businessId) {
        CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
        scheduleSync(bookingId, businessId, "CREATE", eventData);
    }

    public void syncRescheduled(UUID bookingId, UUID businessId) {
        CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
        scheduleSync(bookingId, businessId, "UPDATE", eventData);
    }

    public void syncCancelled(UUID bookingId, UUID businessId) {
        CalendarEventData eventData = loadBookingEventData(bookingId, businessId);
        scheduleSync(bookingId, businessId, "DELETE", eventData);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleSync(UUID bookingId, UUID businessId, String syncAction,
            CalendarEventData eventData) {
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
            Optional<BookingCalendarSyncRecord> existing = syncRepository
                    .findByBookingAndProviderOptional(bookingId, account.provider());
            if (existing.isPresent()) {
                BookingCalendarSyncRecord rec = existing.get();
                if ("SYNCED".equals(rec.syncStatus()) && "DELETE".equals(syncAction)) {
                    performSync(rec, provider, account, eventData);
                } else if ("FAILED".equals(rec.syncStatus())) {
                    performSync(rec, provider, account, eventData);
                } else {
                    LOGGER.debug("CALENDAR_SYNC_SKIP bookingId={} provider={} status={}",
                            bookingId, account.provider(), rec.syncStatus());
                }
                return;
            }
            BookingCalendarSyncRecord record = new BookingCalendarSyncRecord(
                    UUID.randomUUID(), bookingId, businessId, account.provider(),
                    null, "PENDING", syncAction, null, 0, null, null,
                    OffsetDateTime.now(), OffsetDateTime.now());
            syncRepository.insert(record);
            performSync(record, provider, account, eventData);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performSync(BookingCalendarSyncRecord record,
            CalendarProvider provider, CalendarIntegrationAccountRecord account,
            CalendarEventData eventData) {
        if (eventData == null) {
            syncRepository.updateSyncFailed(record.id(), "No hay datos del evento para sincronizar.");
            return;
        }
        try {
            String accessToken = tokenEncryption.decrypt(account.accessTokenEncrypted());
            String externalEventId = record.externalEventId();
            String action = record.syncAction();

            if ("DELETE".equals(action)) {
                if (externalEventId != null) {
                    provider.deleteEvent(externalEventId, accessToken);
                }
                syncRepository.updateSyncSuccess(record.id(), null);
                LOGGER.info("CALENDAR_SYNC_DELETED syncId={} provider={} eventId={}",
                        record.id(), record.provider(), externalEventId);
            } else if ("UPDATE".equals(action) && externalEventId != null) {
                provider.updateEvent(externalEventId, eventData, accessToken);
                syncRepository.updateSyncSuccess(record.id(), externalEventId);
                LOGGER.info("CALENDAR_SYNC_UPDATED syncId={} provider={} eventId={}",
                        record.id(), record.provider(), externalEventId);
            } else {
                String eventId = provider.createEvent(eventData, accessToken);
                syncRepository.updateSyncSuccess(record.id(), eventId);
                LOGGER.info("CALENDAR_SYNC_CREATED syncId={} provider={} eventId={}",
                        record.id(), record.provider(), eventId);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.warn("CALENDAR_SYNC_FAILED syncId={} provider={} reason={}",
                    record.id(), record.provider(), msg);
            syncRepository.updateSyncFailed(record.id(), msg);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retrySync(UUID bookingId) {
        List<BookingCalendarSyncRecord> records = syncRepository.findByBooking(bookingId);
        for (BookingCalendarSyncRecord record : records) {
            if (!"FAILED".equals(record.syncStatus())) continue;
            Optional<CalendarIntegrationAccountRecord> accountOpt = accountRepository
                    .findByBusinessAndProvider(record.businessId(), record.provider());
            if (accountOpt.isEmpty()) {
                LOGGER.warn("CALENDAR_RETRY_NO_ACCOUNT syncId={}", record.id());
                continue;
            }
            CalendarProvider provider = providers.get(record.provider());
            if (provider == null) continue;
            CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
            performSync(record, provider, accountOpt.get(), eventData);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryFailedSyncs() {
        OffsetDateTime before = OffsetDateTime.now().minusMinutes(5);
        List<BookingCalendarSyncRecord> failed = syncRepository.findFailedSyncs(MAX_RETRIES, before);
        for (BookingCalendarSyncRecord record : failed) {
            Optional<CalendarIntegrationAccountRecord> accountOpt = accountRepository
                    .findByBusinessAndProvider(record.businessId(), record.provider());
            if (accountOpt.isEmpty()) continue;
            CalendarProvider provider = providers.get(record.provider());
            if (provider == null) continue;
            CalendarEventData eventData = loadBookingEventData(record.bookingId(), record.businessId());
            performSync(record, provider, accountOpt.get(), eventData);
        }
    }

    public List<BookingCalendarSyncRecord> getSyncStatus(UUID bookingId) {
        return syncRepository.findByBooking(bookingId);
    }

    public boolean hasActiveIntegration(UUID businessId) {
        return !accountRepository.findActiveByBusiness(businessId).isEmpty();
    }

    private CalendarEventData loadBookingEventData(UUID bookingId, UUID businessId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    select
                        b.starts_at,
                        b.ends_at,
                        b.subject as service_name,
                        c.name as customer_name,
                        c.email as customer_email,
                        bl.address as location_address,
                        bl.name as location_name
                    from booking b
                    left join customer c on c.id = b.customer_id
                    left join business_location bl on bl.id = b.location_id
                    where b.id = :bookingId and b.business_id = :businessId
                    """,
                    Map.of("bookingId", bookingId, "businessId", businessId),
                    (rs, rowNum) -> {
                        String loc = rs.getString("location_address");
                        String locName = rs.getString("location_name");
                        String fullLocation = locName != null
                                ? (loc != null ? locName + ", " + loc : locName) : loc;
                        return new CalendarEventData(
                                rs.getString("service_name"),
                                "Reserva: " + rs.getString("service_name")
                                        + " | Cliente: " + rs.getString("customer_name"),
                                fullLocation,
                                rs.getObject("starts_at", OffsetDateTime.class),
                                rs.getObject("ends_at", OffsetDateTime.class),
                                TIMEZONE,
                                rs.getString("customer_email"),
                                rs.getString("customer_name"));
                    });
        } catch (Exception e) {
            LOGGER.warn("CALENDAR_LOAD_BOOKING_FAILED bookingId={} reason={}", bookingId, e.getMessage());
            return null;
        }
    }
}
