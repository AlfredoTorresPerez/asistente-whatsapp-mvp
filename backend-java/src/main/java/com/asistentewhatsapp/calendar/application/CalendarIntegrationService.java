package com.asistentewhatsapp.calendar.application;

import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.TokenExchangeResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarIntegrationService.class);

    private final CalendarIntegrationJdbcRepository repository;
    private final TokenEncryptionService tokenEncryption;
    private final Map<String, CalendarProvider> providers;

    public CalendarIntegrationService(
            CalendarIntegrationJdbcRepository repository,
            TokenEncryptionService tokenEncryption,
            List<CalendarProvider> providerList) {
        this.repository = repository;
        this.tokenEncryption = tokenEncryption;
        Map<String, CalendarProvider> map = new ConcurrentHashMap<>();
        for (CalendarProvider p : providerList) {
            map.put(p.getProviderName(), p);
        }
        this.providers = map;
    }

    public List<CalendarIntegrationAccountRecord> getAccounts(UUID businessId) {
        return repository.findActiveByBusiness(businessId);
    }

    public Optional<CalendarIntegrationAccountRecord> getAccount(UUID accountId) {
        return repository.findById(accountId);
    }

    public String getAuthUrl(UUID businessId, String providerName, String redirectUri) {
        CalendarProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        String state = businessId.toString() + "|" + providerName + "|" + UUID.randomUUID();
        return provider.getAuthUrl(state, redirectUri);
    }

    @Transactional
    public CalendarIntegrationAccountRecord handleOAuthCallback(String state, String code, String redirectUri) {
        String[] parts = state.split("\\|");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid state parameter");
        }
        UUID businessId = UUID.fromString(parts[0]);
        String providerName = parts[1];

        CalendarProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }

        TokenExchangeResult tokenResult = provider.exchangeCode(code, redirectUri);
        String encryptedAccess = tokenEncryption.encrypt(tokenResult.accessToken());
        String encryptedRefresh = tokenEncryption.encrypt(tokenResult.refreshToken());

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = tokenResult.expiresInSeconds() != null
                ? now.plusSeconds(tokenResult.expiresInSeconds())
                : null;

        CalendarIntegrationAccountRecord record = new CalendarIntegrationAccountRecord(
                UUID.randomUUID(), businessId, providerName,
                tokenResult.calendarEmail() != null ? tokenResult.calendarEmail() : "unknown@email.com",
                encryptedAccess, encryptedRefresh, expiresAt,
                tokenResult.calendarId(), null, true, now, now, now);

        repository.save(record);
        LOGGER.info("CALENDAR_ACCOUNT_CONNECTED businessId={} provider={} email={}",
                businessId, providerName, record.email());
        return record;
    }

    @Transactional
    public void disconnect(UUID accountId) {
        Optional<CalendarIntegrationAccountRecord> opt = repository.findById(accountId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        repository.deactivate(accountId);
        LOGGER.info("CALENDAR_ACCOUNT_DISCONNECTED accountId={} provider={}", accountId, opt.get().provider());
    }
}
