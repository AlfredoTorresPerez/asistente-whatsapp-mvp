package com.asistentewhatsapp.calendar.application;

import com.asistentewhatsapp.calendar.api.CalendarAccountResponse;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.CalendarListEntry;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.TokenExchangeResult;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.UserInfoResult;
import com.asistentewhatsapp.security.application.AuditService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarIntegrationService.class);

    private final CalendarIntegrationJdbcRepository repository;
    private final TokenEncryptionService tokenEncryption;
    private final OAuthStateService oAuthStateService;
    private final Map<String, CalendarProvider> providers;
    private final GoogleCalendarHttpClient httpClient;
    private final AuditService auditService;
    private final String frontendBaseUrl;

    public CalendarIntegrationService(
            CalendarIntegrationJdbcRepository repository,
            TokenEncryptionService tokenEncryption,
            OAuthStateService oAuthStateService,
            List<CalendarProvider> providerList,
            GoogleCalendarHttpClient httpClient,
            AuditService auditService,
            @Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.repository = repository;
        this.tokenEncryption = tokenEncryption;
        this.oAuthStateService = oAuthStateService;
        this.httpClient = httpClient;
        this.auditService = auditService;
        this.frontendBaseUrl = frontendBaseUrl;
        Map<String, CalendarProvider> map = new ConcurrentHashMap<>();
        for (CalendarProvider p : providerList) {
            map.put(p.getProviderName(), p);
        }
        this.providers = map;
    }

    public List<CalendarAccountResponse> getStatus(UUID businessId) {
        List<CalendarIntegrationAccountRecord> records = repository.findActiveByBusiness(businessId);
        return records.stream()
                .map(this::toResponse)
                .toList();
    }

    public String getAuthUrl(UUID businessId, String providerName) {
        CalendarProvider provider = getProvider(providerName);
        if (!provider.isEnabled()) {
            throw new IllegalStateException("Calendar provider " + providerName + " is not enabled");
        }
        String state = oAuthStateService.generateState(businessId, providerName);
        String redirectUri = getRedirectUri(providerName);
        return provider.getAuthUrl(state, redirectUri);
    }

    @Transactional
    public CalendarAccountResponse handleOAuthCallback(String state, String code) {
        OAuthStateService.OAuthStateInfo stateInfo = oAuthStateService.consumeAndValidate(state, null, null);

        if (stateInfo == null) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }

        UUID businessId = stateInfo.businessId();
        String providerName = stateInfo.provider();

        CalendarProvider provider = getProvider(providerName);
        if (!provider.isEnabled()) {
            throw new IllegalStateException("Calendar provider " + providerName + " is not enabled");
        }

        String configuredRedirectUri = getRedirectUri(providerName);
        TokenExchangeResult tokenResult = provider.exchangeCode(code, configuredRedirectUri);

        UserInfoResult userInfo = provider.getUserInfo(tokenResult.accessToken());
        String email = userInfo.email() != null ? userInfo.email() : tokenResult.email();

        String encryptedAccess = tokenEncryption.encrypt(tokenResult.accessToken());
        String encryptedRefresh = tokenEncryption.encrypt(tokenResult.refreshToken());

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = tokenResult.expiresInSeconds() != null
                ? now.plusSeconds(tokenResult.expiresInSeconds())
                : null;

        CalendarIntegrationAccountRecord accountRecord = new CalendarIntegrationAccountRecord(
                UUID.randomUUID(), businessId, providerName,
                email,
                encryptedAccess, encryptedRefresh, expiresAt,
                null, null, true, now, now, now,
                null, null, false);

        repository.save(accountRecord);

        auditService.record(businessId, null, "CALENDAR_ACCOUNT_CONNECTED", "CALENDAR", accountRecord.id(),
                "Cuenta de calendario conectada: " + providerName + " - " + CalendarAccountResponse.maskEmail(email));

        LOGGER.info("CALENDAR_ACCOUNT_CONNECTED businessId={} provider={} email={}",
                businessId, providerName, CalendarAccountResponse.maskEmail(email));

        return toResponse(accountRecord);
    }

    @Transactional
    public void disconnect(UUID accountId, UUID businessId) {
        CalendarIntegrationAccountRecord account = findAccountByIdAndBusiness(accountId, businessId);

        String accessToken = null;
        if (account.accessTokenEncrypted() != null) {
            try {
                accessToken = tokenEncryption.decrypt(account.accessTokenEncrypted());
            } catch (Exception e) {
                LOGGER.warn("CALENDAR_DECRYPT_FAILED accountId={}", accountId);
            }
        }

        if (accessToken != null) {
            try {
                CalendarProvider provider = getProvider(account.provider());
                provider.revokeToken(accessToken);
            } catch (Exception e) {
                LOGGER.warn("CALENDAR_REVOKE_FAILED accountId={} reason={}", accountId, e.getMessage());
            }
        }

        repository.revokeAccount(accountId, businessId);

        auditService.record(businessId, null, "CALENDAR_ACCOUNT_DISCONNECTED", "CALENDAR", accountId,
                "Cuenta de calendario desvinculada: " + account.provider());

        LOGGER.info("CALENDAR_ACCOUNT_DISCONNECTED accountId={} provider={}", accountId, account.provider());
    }

    @Transactional
    public void selectCalendar(UUID accountId, UUID businessId, String calendarId, String calendarSummary) {
        CalendarIntegrationAccountRecord account = findAccountByIdAndBusiness(accountId, businessId);

        String accessToken = tokenEncryption.decrypt(account.accessTokenEncrypted());
        CalendarProvider provider = getProvider(account.provider());

        List<CalendarListEntry> available = provider.listCalendars(accessToken);
        boolean found = false;
        for (CalendarListEntry entry : available) {
            if (entry.id().equals(calendarId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Calendar with id " + calendarId
                    + " is not available or not writable for this account");
        }

        repository.updateCalendarId(accountId, businessId, calendarId, calendarSummary);

        auditService.record(businessId, null, "CALENDAR_SELECTED", "CALENDAR", accountId,
                "Calendario seleccionado: " + calendarSummary + " (" + calendarId + ")");

        LOGGER.info("CALENDAR_SELECTED accountId={} calendarId={} summary={}", accountId, calendarId, calendarSummary);
    }

    public List<CalendarListEntry> listCalendars(UUID accountId, UUID businessId) {
        CalendarIntegrationAccountRecord account = findAccountByIdAndBusiness(accountId, businessId);
        String accessToken = tokenEncryption.decrypt(account.accessTokenEncrypted());
        CalendarProvider provider = getProvider(account.provider());
        return provider.listCalendars(accessToken);
    }

    public CalendarAccountResponse getAccountByIdAndBusiness(UUID accountId, UUID businessId) {
        CalendarIntegrationAccountRecord account = findAccountByIdAndBusiness(accountId, businessId);
        return toResponse(account);
    }

    public boolean isIntegrationActive(UUID businessId) {
        return !repository.findActiveByBusiness(businessId).isEmpty();
    }

    public CalendarIntegrationAccountRecord findAccountByIdAndBusiness(UUID accountId, UUID businessId) {
        return repository.findByIdAndBusiness(accountId, businessId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Calendar account not found: " + accountId));
    }

    private CalendarProvider getProvider(String providerName) {
        CalendarProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown calendar provider: " + providerName);
        }
        return provider;
    }

    private String getRedirectUri(String providerName) {
        CalendarProvider provider = getProvider(providerName);
        // The provider uses its own redirectUri as fallback
        return null;
    }

    private CalendarAccountResponse toResponse(CalendarIntegrationAccountRecord record) {
        String status = CalendarAccountResponse.determineAuthorizationStatus(
                record.active(), record.requiresReconnect(), record.revokedAt());
        return new CalendarAccountResponse(
                record.id(),
                record.provider(),
                CalendarAccountResponse.maskEmail(record.email()),
                record.calendarId(),
                record.calendarSummary(),
                record.active(),
                record.connectedAt(),
                record.lastSyncAt(),
                record.requiresReconnect(),
                record.revokedAt(),
                status);
    }
}
