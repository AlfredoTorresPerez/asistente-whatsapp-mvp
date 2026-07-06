package com.asistentewhatsapp.calendar.provider;

import com.asistentewhatsapp.calendar.CalendarEventData;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCalendarProvider implements CalendarProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarProvider.class);
    private static final String PROVIDER_NAME = "GOOGLE";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final boolean enabled;

    public GoogleCalendarProvider(
            @Value("${app.calendar.google.client-id:}") String clientId,
            @Value("${app.calendar.google.client-secret:}") String clientSecret,
            @Value("${app.calendar.google.redirect-uri:}") String redirectUri,
            @Value("${app.calendar.google.enabled:false}") boolean enabled) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.enabled = enabled;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAuthUrl(String state, String frontendRedirectUri) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("https://www.googleapis.com/auth/calendar.events", StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + state;
    }

    @Override
    public TokenExchangeResult exchangeCode(String code, String ignored) {
        if (!enabled) {
            LOGGER.info("GOOGLE_CALENDAR_SIMULATED codeExchange codeMasked={}", mask(code));
            return simulatedExchange();
        }
        LOGGER.info("GOOGLE_CALENDAR_TOKEN_EXCHANGE codeMasked={} redirectUri={}", mask(code), redirectUri);
        return simulatedExchange();
    }

    @Override
    public RefreshResult refreshAccessToken(String refreshToken) {
        if (!enabled) {
            LOGGER.info("GOOGLE_CALENDAR_SIMULATED tokenRefresh");
            return new RefreshResult("simulated_access_token_" + System.currentTimeMillis(), 3600L);
        }
        LOGGER.info("GOOGLE_CALENDAR_TOKEN_REFRESH");
        return new RefreshResult("refreshed_token_" + System.currentTimeMillis(), 3600L);
    }

    @Override
    public String createEvent(CalendarEventData eventData, String accessToken) {
        LOGGER.info("GOOGLE_CALENDAR_CREATE_EVENT summary={} start={} end={}",
                eventData.summary(), eventData.startAt(), eventData.endAt());
        return "google_event_" + System.currentTimeMillis();
    }

    @Override
    public void updateEvent(String externalEventId, CalendarEventData eventData, String accessToken) {
        LOGGER.info("GOOGLE_CALENDAR_UPDATE_EVENT eventId={} summary={} start={} end={}",
                externalEventId, eventData.summary(), eventData.startAt(), eventData.endAt());
    }

    @Override
    public void deleteEvent(String externalEventId, String accessToken) {
        LOGGER.info("GOOGLE_CALENDAR_DELETE_EVENT eventId={}", externalEventId);
    }

    private TokenExchangeResult simulatedExchange() {
        return new TokenExchangeResult(
                "simulated_access_token_" + System.currentTimeMillis(),
                "simulated_refresh_token_" + System.currentTimeMillis(),
                3600L,
                "calendar@demo.cl",
                "primary");
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
