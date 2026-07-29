package com.asistentewhatsapp.calendar.provider;

import com.asistentewhatsapp.calendar.CalendarEventData;
import java.util.List;

public interface CalendarProvider {
	String getProviderName();
	boolean isEnabled();
	String getAuthUrl(String state, String redirectUri);
	TokenExchangeResult exchangeCode(String code, String redirectUri);
	RefreshResult refreshAccessToken(String refreshToken);
	RevokeResult revokeToken(String accessToken);
	UserInfoResult getUserInfo(String accessToken);
	List<CalendarListEntry> listCalendars(String accessToken);
	String createEvent(String calendarId, CalendarEventData eventData, String accessToken);
	void updateEvent(String calendarId, String externalEventId, CalendarEventData eventData, String accessToken);
	void deleteEvent(String calendarId, String externalEventId, String accessToken);
	CalendarEventData getEvent(String calendarId, String externalEventId, String accessToken);

	record TokenExchangeResult(String accessToken, String refreshToken, Long expiresInSeconds, String email,
			String name) {
	}
	record RefreshResult(String accessToken, Long expiresInSeconds) {
	}
	record RevokeResult(boolean success, String errorMessage) {
	}
	record UserInfoResult(String id, String email, boolean verifiedEmail, String name) {
	}
	record CalendarListEntry(String id, String summary, boolean primary, String accessRole) {
	}
}
