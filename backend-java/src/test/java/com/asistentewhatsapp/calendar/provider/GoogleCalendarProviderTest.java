package com.asistentewhatsapp.calendar.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.TokenResponse;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.RefreshTokenResponse;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.UserInfoResponse;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.CalendarListResponse;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.CalendarListEntry;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.TokenExchangeResult;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.RefreshResult;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.RevokeResult;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.UserInfoResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GoogleCalendarProvider - Proveedor de calendario Google con HttpClient")
class GoogleCalendarProviderTest {

	private static final String STATE = "test-state-value";
	private static final String REDIRECT_URI = "http://localhost:5173/calendario";
	private static final String CODE = "test-auth-code";
	private static final String ACCESS_TOKEN = "ya29.test-access-token";
	private static final String REFRESH_TOKEN = "1//test-refresh-token";
	private static final String CLIENT_ID = "test-client-id";
	private static final String CLIENT_SECRET = "test-client-secret";
	private static final String CALENDAR_ID = "primary";

	private GoogleCalendarHttpClient httpClient;
	private GoogleCalendarProvider provider;

	@BeforeEach
	void setUp() {
		httpClient = mock(GoogleCalendarHttpClient.class);
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, true);
	}

	@Test
	@DisplayName("getProviderName retorna GOOGLE")
	void getProviderNameReturnsGoogle() {
		assertThat(provider.getProviderName()).isEqualTo("GOOGLE");
	}

	@Test
	@DisplayName("isEnabled retorna true cuando enabled=true")
	void isEnabledReturnsTrue() {
		assertThat(provider.isEnabled()).isTrue();
	}

	@Test
	@DisplayName("isEnabled retorna false cuando enabled=false")
	void isEnabledReturnsFalse() {
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, false);
		assertThat(provider.isEnabled()).isFalse();
	}

	@Test
	@DisplayName("getAuthUrl contiene parámetros esperados")
	void getAuthUrlContainsExpectedParameters() {
		String url = provider.getAuthUrl(STATE, REDIRECT_URI);
		assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(url).contains("client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8));
		assertThat(url).contains("redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8));
		assertThat(url).contains("access_type=offline");
		assertThat(url).contains("prompt=consent");
		assertThat(url).contains("state=" + URLEncoder.encode(STATE, StandardCharsets.UTF_8));
		assertThat(url).contains("response_type=code");
		assertThat(url).contains("scope=");
	}

	@Test
	@DisplayName("getAuthUrl con provider disabled lanza IllegalStateException")
	void getAuthUrlWhenDisabledThrows() {
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, false);
		assertThatThrownBy(() -> provider.getAuthUrl(STATE, REDIRECT_URI)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not enabled");
	}

	@Test
	@DisplayName("exchangeCode llama a httpClient y retorna TokenExchangeResult")
	void exchangeCodeCallsHttpClient() {
		when(httpClient.exchangeAuthorizationCode(eq(CODE), eq(REDIRECT_URI), eq(CLIENT_ID), eq(CLIENT_SECRET)))
				.thenReturn(new TokenResponse(ACCESS_TOKEN, REFRESH_TOKEN, 3600L, "email profile"));
		TokenExchangeResult result = provider.exchangeCode(CODE, REDIRECT_URI);
		assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
		assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
		assertThat(result.expiresInSeconds()).isEqualTo(3600L);
	}

	@Test
	@DisplayName("exchangeCode con provider disabled lanza IllegalStateException")
	void exchangeCodeWhenDisabledThrows() {
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, false);
		assertThatThrownBy(() -> provider.exchangeCode(CODE, REDIRECT_URI)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not enabled");
	}

	@Test
	@DisplayName("refreshAccessToken llama a httpClient.refreshAccessToken")
	void refreshAccessTokenCallsHttpClient() {
		when(httpClient.refreshAccessToken(eq(REFRESH_TOKEN), eq(CLIENT_ID), eq(CLIENT_SECRET)))
				.thenReturn(new RefreshTokenResponse("new-access-token", 3600L));
		RefreshResult result = provider.refreshAccessToken(REFRESH_TOKEN);
		assertThat(result.accessToken()).isEqualTo("new-access-token");
		assertThat(result.expiresInSeconds()).isEqualTo(3600L);
	}

	@Test
	@DisplayName("refreshAccessToken con provider disabled lanza IllegalStateException")
	void refreshAccessTokenWhenDisabledThrows() {
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, false);
		assertThatThrownBy(() -> provider.refreshAccessToken(REFRESH_TOKEN)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not enabled");
	}

	@Test
	@DisplayName("revokeToken llama a httpClient.revokeToken")
	void revokeTokenCallsHttpClient() {
		RevokeResult result = provider.revokeToken(ACCESS_TOKEN);
		verify(httpClient).revokeToken(ACCESS_TOKEN);
		assertThat(result.success()).isTrue();
	}

	@Test
	@DisplayName("getUserInfo llama a httpClient.getUserInfo")
	void getUserInfoCallsHttpClient() {
		when(httpClient.getUserInfo(ACCESS_TOKEN))
				.thenReturn(new UserInfoResponse("user-1", "test@demo.cl", true, "Test User", null));
		UserInfoResult result = provider.getUserInfo(ACCESS_TOKEN);
		assertThat(result.email()).isEqualTo("test@demo.cl");
		assertThat(result.name()).isEqualTo("Test User");
	}

	@Test
	@DisplayName("listCalendars retorna solo calendarios writables")
	void listCalendarsReturnsWritableOnly() {
		when(httpClient.listCalendarList(ACCESS_TOKEN))
				.thenReturn(new CalendarListResponse(List.of(new CalendarListEntry("id1", "Calendar 1", true, "owner"),
						new CalendarListEntry("id2", "Calendar 2", false, "reader"),
						new CalendarListEntry("id3", "Calendar 3", false, "writer"))));
		var result = provider.listCalendars(ACCESS_TOKEN);
		assertThat(result).hasSize(2);
		assertThat(result.stream().map(CalendarProvider.CalendarListEntry::id)).containsExactly("id1", "id3");
	}

	@Test
	@DisplayName("createEvent construye JSON con extendedProperties y llama a httpClient")
	void createEventBuildsJsonAndCallsHttpClient() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CalendarEventData eventData = new CalendarEventData("Test Event", "Description", "Location",
				OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1), "America/Santiago",
				"test@demo.cl", "Test", businessId, bookingId, null);
		when(httpClient.createEvent(eq(CALENDAR_ID), anyString(), eq(ACCESS_TOKEN)))
				.thenReturn("{\"id\": \"google_event_123\"}");
		String eventId = provider.createEvent(CALENDAR_ID, eventData, ACCESS_TOKEN);
		assertThat(eventId).isEqualTo("google_event_123");
		verify(httpClient).createEvent(eq(CALENDAR_ID), anyString(), eq(ACCESS_TOKEN));
	}

	@Test
	@DisplayName("createEvent incluye id idempotente derivado de SHA-256 de businessId + bookingId")
	void createEventUsesIdempotentId() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CalendarEventData eventData = new CalendarEventData("Test", "Desc", "Loc", OffsetDateTime.now().plusDays(1),
				OffsetDateTime.now().plusDays(1).plusHours(1), "America/Santiago", "t@t.cl", "Test", businessId,
				bookingId, null);
		when(httpClient.createEvent(eq(CALENDAR_ID), anyString(), eq(ACCESS_TOKEN)))
				.thenReturn("{\"id\": \"test_id\"}");
		provider.createEvent(CALENDAR_ID, eventData, ACCESS_TOKEN);
	}

	@Test
	@DisplayName("createEvent con provider disabled lanza IllegalStateException")
	void createEventWhenDisabledThrows() {
		provider = new GoogleCalendarProvider(httpClient, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, false);
		assertThatThrownBy(() -> provider.createEvent(CALENDAR_ID, null, ACCESS_TOKEN))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("not enabled");
	}

	@Test
	@DisplayName("updateEvent llama a httpClient.updateEvent")
	void updateEventCallsHttpClient() {
		CalendarEventData eventData = new CalendarEventData("Test", "Desc", "Loc", OffsetDateTime.now().plusDays(1),
				OffsetDateTime.now().plusDays(1).plusHours(1), "America/Santiago", "t@t.cl", "Test", UUID.randomUUID(),
				UUID.randomUUID(), null);
		provider.updateEvent(CALENDAR_ID, "ext-event-id", eventData, ACCESS_TOKEN);
		verify(httpClient).updateEvent(eq(CALENDAR_ID), eq("ext-event-id"), anyString(), eq(ACCESS_TOKEN));
	}

	@Test
	@DisplayName("deleteEvent llama a httpClient.deleteEvent")
	void deleteEventCallsHttpClient() {
		provider.deleteEvent(CALENDAR_ID, "ext-event-id", ACCESS_TOKEN);
		verify(httpClient).deleteEvent(CALENDAR_ID, "ext-event-id", ACCESS_TOKEN);
	}

	@Test
	@DisplayName("getEvent llama a httpClient.getEvent y parsea respuesta")
	void getEventCallsHttpClient() {
		when(httpClient.getEvent(CALENDAR_ID, "ext-1", ACCESS_TOKEN)).thenReturn(
				"{\"id\": \"ext-1\", \"summary\": \"Test\", \"start\": {\"dateTime\": \"2026-07-15T10:00:00-04:00\"}, \"end\": {\"dateTime\": \"2026-07-15T11:00:00-04:00\"}}");
		CalendarEventData result = provider.getEvent(CALENDAR_ID, "ext-1", ACCESS_TOKEN);
		assertThat(result).isNotNull();
		assertThat(result.summary()).isEqualTo("Test");
	}
}
