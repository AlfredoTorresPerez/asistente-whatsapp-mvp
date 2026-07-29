package com.asistentewhatsapp.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.calendar.api.CalendarAccountResponse;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.TokenExchangeResult;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.UserInfoResult;
import com.asistentewhatsapp.security.application.AuditService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalendarIntegrationService - Servicio de integración de calendario")
class CalendarIntegrationServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final UUID ACCOUNT_ID = UUID.randomUUID();
	private static final String PROVIDER_NAME = "GOOGLE";

	private CalendarIntegrationJdbcRepository repository;
	private TokenEncryptionService tokenEncryption;
	private OAuthStateService oAuthStateService;
	private CalendarProvider provider;
	private GoogleCalendarHttpClient httpClient;
	private AuditService auditService;
	private CalendarIntegrationService service;

	@BeforeEach
	void setUp() {
		repository = mock(CalendarIntegrationJdbcRepository.class);
		tokenEncryption = mock(TokenEncryptionService.class);
		oAuthStateService = mock(OAuthStateService.class);
		provider = mock(CalendarProvider.class);
		httpClient = mock(GoogleCalendarHttpClient.class);
		auditService = mock(AuditService.class);
		when(provider.getProviderName()).thenReturn(PROVIDER_NAME);
		when(provider.isEnabled()).thenReturn(true);
		when(provider.getAuthUrl(anyString(), nullable(String.class)))
				.thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");
		when(provider.exchangeCode(anyString(), nullable(String.class)))
				.thenReturn(new TokenExchangeResult("access-token", "refresh-token", 3600L, "test@demo.cl", null));
		when(provider.getUserInfo(anyString()))
				.thenReturn(new UserInfoResult("id-1", "test@demo.cl", true, "Test User"));
		when(tokenEncryption.encrypt(anyString())).thenReturn("encrypted_value");
		service = new CalendarIntegrationService(repository, tokenEncryption, oAuthStateService, List.of(provider),
				httpClient, auditService, "http://localhost:5173");
	}

	@Test
	@DisplayName("getStatus retorna lista de CalendarAccountResponse sin tokens")
	void getStatusReturnsSafeDto() {
		when(repository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		List<CalendarAccountResponse> result = service.getStatus(BUSINESS_ID);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().emailMasked()).contains("***");
		assertThat(result.getFirst().provider()).isEqualTo(PROVIDER_NAME);
	}

	@Test
	@DisplayName("getAuthUrl genera estado y retorna URL")
	void getAuthUrlReturnsUrl() {
		when(oAuthStateService.generateState(BUSINESS_ID, PROVIDER_NAME)).thenReturn("test-state");
		String url = service.getAuthUrl(BUSINESS_ID, PROVIDER_NAME);
		assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
		verify(oAuthStateService).generateState(BUSINESS_ID, PROVIDER_NAME);
	}

	@Test
	@DisplayName("getAuthUrl con provider desconocido lanza excepción")
	void getAuthUrlWithUnknownProviderThrows() {
		assertThatThrownBy(() -> service.getAuthUrl(BUSINESS_ID, "UNKNOWN"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown calendar provider");
	}

	@Test
	@DisplayName("getAuthUrl con provider disabled lanza IllegalStateException")
	void getAuthUrlWithDisabledProviderThrows() {
		when(provider.isEnabled()).thenReturn(false);
		assertThatThrownBy(() -> service.getAuthUrl(BUSINESS_ID, PROVIDER_NAME))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("not enabled");
	}

	@Test
	@DisplayName("handleOAuthCallback intercambia código, obtiene userInfo, cifra tokens y guarda")
	void handleOAuthCallbackExchangesCodeAndSaves() {
		when(oAuthStateService.consumeAndValidate(anyString(), any(), any()))
				.thenReturn(new OAuthStateService.OAuthStateInfo(BUSINESS_ID, PROVIDER_NAME, null));
		CalendarAccountResponse response = service.handleOAuthCallback("state", "code");
		verify(provider).exchangeCode(anyString(), nullable(String.class));
		verify(provider).getUserInfo(anyString());
		verify(tokenEncryption).encrypt("access-token");
		verify(tokenEncryption).encrypt("refresh-token");
		verify(repository).save(any());
		verify(auditService).record(any(), any(), anyString(), anyString(), any(), anyString());
		assertThat(response.authorizationStatus()).isEqualTo("CONNECTED");
	}

	@Test
	@DisplayName("handleOAuthCallback con state inválido lanza excepción")
	void handleOAuthCallbackWithInvalidStateThrows() {
		when(oAuthStateService.consumeAndValidate(anyString(), any(), any()))
				.thenThrow(new IllegalArgumentException("Invalid OAuth state"));
		assertThatThrownBy(() -> service.handleOAuthCallback("bad-state", "code"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("disconnect revoca token y revoca cuenta")
	void disconnectRevokesTokenAndRevokesAccount() {
		CalendarIntegrationAccountRecord account = activeAccount();
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.of(account));
		when(tokenEncryption.decrypt(account.accessTokenEncrypted())).thenReturn("decrypted_access");
		service.disconnect(ACCOUNT_ID, BUSINESS_ID);
		verify(provider).revokeToken("decrypted_access");
		verify(repository).revokeAccount(ACCOUNT_ID, BUSINESS_ID);
		verify(auditService).record(any(), any(), anyString(), anyString(), any(), anyString());
	}

	@Test
	@DisplayName("disconnect con businessId incorrecto lanza excepción")
	void disconnectWithWrongBusinessThrows() {
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.disconnect(ACCOUNT_ID, BUSINESS_ID))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not found");
	}

	@Test
	@DisplayName("selectCalendar valida que el calendario pertenezca a la cuenta y sea writable")
	void selectCalendarValidatesCalendar() {
		CalendarIntegrationAccountRecord account = activeAccount();
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.of(account));
		when(tokenEncryption.decrypt(anyString())).thenReturn("decrypted_access");
		when(provider.listCalendars("decrypted_access"))
				.thenReturn(List.of(new CalendarProvider.CalendarListEntry("cal-1", "My Calendar", true, "owner")));
		service.selectCalendar(ACCOUNT_ID, BUSINESS_ID, "cal-1", "My Calendar");
		verify(repository).updateCalendarId(ACCOUNT_ID, BUSINESS_ID, "cal-1", "My Calendar");
		verify(auditService).record(any(), any(), anyString(), anyString(), any(), anyString());
	}

	@Test
	@DisplayName("selectCalendar con calendario no disponible lanza excepción")
	void selectCalendarWithUnavailableCalendarThrows() {
		CalendarIntegrationAccountRecord account = activeAccount();
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.of(account));
		when(tokenEncryption.decrypt(anyString())).thenReturn("decrypted_access");
		when(provider.listCalendars("decrypted_access")).thenReturn(List.of());
		assertThatThrownBy(() -> service.selectCalendar(ACCOUNT_ID, BUSINESS_ID, "unknown-cal", "Unknown"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not available");
	}

	@Test
	@DisplayName("listCalendars retorna entries del provider")
	void listCalendarsReturnsEntries() {
		CalendarIntegrationAccountRecord account = activeAccount();
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.of(account));
		when(tokenEncryption.decrypt(anyString())).thenReturn("decrypted_access");
		when(provider.listCalendars("decrypted_access"))
				.thenReturn(List.of(new CalendarProvider.CalendarListEntry("cal-1", "Calendar", true, "owner")));
		var result = service.listCalendars(ACCOUNT_ID, BUSINESS_ID);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo("cal-1");
	}

	@Test
	@DisplayName("isIntegrationActive retorna true cuando hay cuentas activas")
	void isIntegrationActiveReturnsTrue() {
		when(repository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		assertThat(service.isIntegrationActive(BUSINESS_ID)).isTrue();
	}

	@Test
	@DisplayName("isIntegrationActive retorna false cuando no hay cuentas")
	void isIntegrationActiveReturnsFalse() {
		when(repository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of());
		assertThat(service.isIntegrationActive(BUSINESS_ID)).isFalse();
	}

	@Test
	@DisplayName("getAccountByIdAndBusiness retorna response sin tokens")
	void getAccountByIdAndBusinessReturnsSafeDto() {
		CalendarIntegrationAccountRecord account = activeAccount();
		when(repository.findByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID)).thenReturn(Optional.of(account));
		CalendarAccountResponse response = service.getAccountByIdAndBusiness(ACCOUNT_ID, BUSINESS_ID);
		assertThat(response.emailMasked()).isNotNull();
		assertThat(response.authorizationStatus()).isEqualTo("CONNECTED");
	}

	private CalendarIntegrationAccountRecord activeAccount() {
		return new CalendarIntegrationAccountRecord(ACCOUNT_ID, BUSINESS_ID, PROVIDER_NAME, "test@demo.cl",
				"encrypted_access_token", "encrypted_refresh_token", OffsetDateTime.now().plusHours(1), "primary",
				"Calendar", true, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), null,
				OffsetDateTime.now(), false);
	}
}
