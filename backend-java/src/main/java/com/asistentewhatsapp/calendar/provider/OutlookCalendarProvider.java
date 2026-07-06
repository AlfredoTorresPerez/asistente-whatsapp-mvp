package com.asistentewhatsapp.calendar.provider;

import com.asistentewhatsapp.calendar.CalendarEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutlookCalendarProvider implements CalendarProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookCalendarProvider.class);
    private static final String PROVIDER_NAME = "OUTLOOK";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAuthUrl(String state, String redirectUri) {
        LOGGER.info("OUTLOOK_CALENDAR_AUTH_URL state={}", state);
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
                + "?client_id=OUTLOOK_CLIENT_ID_NOT_CONFIGURED"
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=Calendars.ReadWrite"
                + "&state=" + state;
    }

    @Override
    public TokenExchangeResult exchangeCode(String code, String redirectUri) {
        LOGGER.info("OUTLOOK_CALENDAR_STUB exchangeCode codeMasked={}", mask(code));
        return new TokenExchangeResult(
                "outlook_stub_access_" + System.currentTimeMillis(),
                "outlook_stub_refresh_" + System.currentTimeMillis(),
                3600L,
                "outlook@demo.cl",
                "outlook_calendar_id");
    }

    @Override
    public RefreshResult refreshAccessToken(String refreshToken) {
        LOGGER.info("OUTLOOK_CALENDAR_STUB tokenRefresh");
        return new RefreshResult("outlook_stub_refreshed_" + System.currentTimeMillis(), 3600L);
    }

    @Override
    public String createEvent(CalendarEventData eventData, String accessToken) {
        LOGGER.info("OUTLOOK_CALENDAR_STUB createEvent summary={}", eventData.summary());
        return "outlook_event_" + System.currentTimeMillis();
    }

    @Override
    public void updateEvent(String externalEventId, CalendarEventData eventData, String accessToken) {
        LOGGER.info("OUTLOOK_CALENDAR_STUB updateEvent eventId={}", externalEventId);
    }

    @Override
    public void deleteEvent(String externalEventId, String accessToken) {
        LOGGER.info("OUTLOOK_CALENDAR_STUB deleteEvent eventId={}", externalEventId);
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
