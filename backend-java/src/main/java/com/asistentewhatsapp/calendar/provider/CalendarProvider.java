package com.asistentewhatsapp.calendar.provider;

import com.asistentewhatsapp.calendar.CalendarEventData;

public interface CalendarProvider {

    String getProviderName();

    String getAuthUrl(String state, String redirectUri);

    TokenExchangeResult exchangeCode(String code, String redirectUri);

    RefreshResult refreshAccessToken(String refreshToken);

    String createEvent(CalendarEventData eventData, String accessToken);

    void updateEvent(String externalEventId, CalendarEventData eventData, String accessToken);

    void deleteEvent(String externalEventId, String accessToken);

    record TokenExchangeResult(String accessToken, String refreshToken, Long expiresInSeconds, String calendarEmail, String calendarId) {}

    record RefreshResult(String accessToken, Long expiresInSeconds) {}
}
