package com.asistentewhatsapp.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.GoogleCalendarApiException;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.RefreshResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@DisplayName("CalendarSyncService - Sincronización de reservas con calendario externo")
class CalendarSyncServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final UUID BOOKING_ID = UUID.randomUUID();
	private static final String PROVIDER_NAME = "GOOGLE";
	private static final String CALENDAR_ID = "primary";
	private static final int MAX_RETRIES = 5;
	private static final long RETRY_INTERVAL_MS = 300000L;

	private BookingCalendarSyncJdbcRepository syncRepository;
	private CalendarIntegrationJdbcRepository accountRepository;
	private TokenEncryptionService tokenEncryption;
	private CalendarProvider provider;
	private CalendarProvider outlookProvider;
	private CalendarSyncService service;
	private NamedParameterJdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		syncRepository = mock(BookingCalendarSyncJdbcRepository.class);
		accountRepository = mock(CalendarIntegrationJdbcRepository.class);
		tokenEncryption = mock(TokenEncryptionService.class);
		provider = mock(CalendarProvider.class);
		when(provider.getProviderName()).thenReturn(PROVIDER_NAME);
		when(tokenEncryption.decrypt(anyString())).thenReturn("decrypted_access_token");
		when(tokenEncryption.encrypt(anyString())).thenReturn("encrypted_result");
		outlookProvider = mock(CalendarProvider.class);
		when(outlookProvider.getProviderName()).thenReturn("OUTLOOK");
		jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		service = new CalendarSyncService(syncRepository, accountRepository, tokenEncryption,
				List.of(provider, outlookProvider), jdbcTemplate, MAX_RETRIES, RETRY_INTERVAL_MS);
	}

	private void mockEventData() {
		when(jdbcTemplate.queryForObject(anyString(), any(java.util.Map.class), any(RowMapper.class)))
				.thenReturn(new CalendarEventData("Test", "Desc", "Loc", OffsetDateTime.now().plusDays(1),
						OffsetDateTime.now().plusDays(1).plusHours(1), "America/Santiago", "test@demo.cl", "Test",
						BUSINESS_ID, BOOKING_ID, null));
	}

	@Test
	@DisplayName("syncConfirmed crea registro PENDING sin llamar al provider")
	void syncConfirmedCreatesPendingRecord() {
		mockEventData();
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME)).thenReturn(Optional.empty());

		service.syncConfirmed(BOOKING_ID, BUSINESS_ID);

		verify(syncRepository).insert(any());
		verify(provider, never()).createEvent(any(), any(), any());
	}

	@Test
	@DisplayName("syncConfirmed cuando no hay cuentas no hace nada")
	void syncConfirmedWhenNoAccountsDoesNothing() {
		mockEventData();
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of());
		service.syncConfirmed(BOOKING_ID, BUSINESS_ID);
		verify(syncRepository, never()).insert(any());
	}

	@Test
	@DisplayName("processPendingSyncs procesa registro PENDING y llama a createEvent")
	void processPendingSyncsCreateEvent() {
		mockEventData();
		BookingCalendarSyncRecord pending = pendingRecord("CREATE");
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pending));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(activeAccount()));
		when(provider.createEvent(eq(CALENDAR_ID), any(), any())).thenReturn("google_event_123");

		service.processPendingSyncs();

		verify(provider).createEvent(eq(CALENDAR_ID), any(), any());
		verify(syncRepository).updateSyncSuccess(any(), anyString());
	}

	@Test
	@DisplayName("syncCancelled con SYNCED existente llama a deleteEvent en performSync")
	void syncCancelledDeletesEvent() {
		mockEventData();
		BookingCalendarSyncRecord synced = syncedRecord("DELETE");
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(synced));

		service.syncCancelled(BOOKING_ID, BUSINESS_ID);

		verify(provider).deleteEvent(eq(CALENDAR_ID), eq("ext_event_1"), any());
	}

	@Test
	@DisplayName("syncCancelled con externalEventId nulo no llama a deleteEvent")
	void syncCancelledWithNullExternalIdDoesNotCallProvider() {
		mockEventData();
		BookingCalendarSyncRecord synced = new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID, BUSINESS_ID,
				PROVIDER_NAME, null, "SYNCED", "DELETE", null, 0, OffsetDateTime.now(), OffsetDateTime.now(),
				OffsetDateTime.now(), OffsetDateTime.now());
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(synced));

		service.syncCancelled(BOOKING_ID, BUSINESS_ID);

		verify(provider, never()).deleteEvent(any(), any(), any());
	}

	@Test
	@DisplayName("processPendingSyncs con error del provider marca FAILED")
	void processPendingSyncsMarksFailedOnError() {
		mockEventData();
		BookingCalendarSyncRecord pending = pendingRecord("CREATE");
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pending));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(activeAccount()));
		when(provider.createEvent(eq(CALENDAR_ID), any(), any()))
				.thenThrow(new GoogleCalendarApiException(500, "Error interno", null));

		service.processPendingSyncs();

		verify(provider).createEvent(eq(CALENDAR_ID), any(), any());
		verify(syncRepository).updateSyncFailed(any(), anyString());
	}

	@Test
	@DisplayName("processPendingSyncs con DELETE y 404 marca como CANCELLED")
	void processPendingSyncsDeleteNotFoundMarksCancelled() {
		mockEventData();
		BookingCalendarSyncRecord pending = pendingRecord("DELETE", "ext_event_1");
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pending));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(activeAccount()));
		doThrow(new GoogleCalendarApiException(404, "Not found", null)).when(provider).deleteEvent(eq(CALENDAR_ID),
				eq("ext_event_1"), any());

		service.processPendingSyncs();

		verify(syncRepository).updateStatus(any(), eq("CANCELLED"), any(), any());
	}

	@Test
	@DisplayName("processPendingSyncs con 401 refresca token y reintenta")
	void processPendingSyncsAuthErrorRefreshesToken() {
		mockEventData();
		CalendarIntegrationAccountRecord account = activeAccountWithRefresh();
		BookingCalendarSyncRecord pending = pendingRecord("CREATE");
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pending));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME)).thenReturn(Optional.of(account));
		when(provider.createEvent(eq(CALENDAR_ID), any(), any()))
				.thenThrow(new GoogleCalendarApiException(401, "Unauthorized", null));
		when(provider.refreshAccessToken(anyString())).thenReturn(new RefreshResult("new_access", 3600L));
		when(accountRepository.findByIdAndBusiness(any(), any())).thenReturn(Optional.of(account));

		service.processPendingSyncs();

		verify(provider, times(2)).refreshAccessToken(anyString());
		verify(accountRepository, times(2)).updateTokens(any(), any(), anyString(), anyString(), any());
	}

	@Test
	@DisplayName("processPendingSyncs con 403 marca requiresReconnect en la cuenta")
	void processPendingSyncsPermanent403MarksRequiresReconnect() {
		mockEventData();
		CalendarIntegrationAccountRecord account = activeAccount();
		BookingCalendarSyncRecord pending = pendingRecord("CREATE");
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pending));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME)).thenReturn(Optional.of(account));
		when(provider.createEvent(eq(CALENDAR_ID), any(), any()))
				.thenThrow(new GoogleCalendarApiException(403, "Forbidden", null));

		service.processPendingSyncs();

		verify(accountRepository).updateRequiresReconnect(eq(account.id()), eq(BUSINESS_ID), eq(true));
	}

	@Test
	@DisplayName("syncRescheduled con SYNCED existente crea nuevo PENDING sin llamar al provider")
	void syncRescheduledWithSyncedCreatesPending() {
		mockEventData();
		BookingCalendarSyncRecord synced = syncedRecord("UPDATE");
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(synced));

		service.syncRescheduled(BOOKING_ID, BUSINESS_ID);

		verify(syncRepository).insert(any());
		verify(provider, never()).updateEvent(any(), any(), any(), any());
	}

	@Test
	@DisplayName("Múltiples providers son procesados en processPendingSyncs")
	void multipleProvidersAreBothProcessed() {
		mockEventData();
		CalendarIntegrationAccountRecord googleAccount = activeAccount();
		CalendarIntegrationAccountRecord outlookAccount = new CalendarIntegrationAccountRecord(UUID.randomUUID(),
				BUSINESS_ID, "OUTLOOK", "outlook@demo.cl", "enc_access", "enc_refresh",
				OffsetDateTime.now().plusHours(1), "outlook_cal", "Outlook Calendar", true, OffsetDateTime.now(),
				OffsetDateTime.now(), OffsetDateTime.now(), null, OffsetDateTime.now(), false);

		BookingCalendarSyncRecord pendingGoogle = pendingRecord("CREATE");
		BookingCalendarSyncRecord pendingOutlook = new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID,
				BUSINESS_ID, "OUTLOOK", null, "PENDING", "CREATE", null, 0, null, null, OffsetDateTime.now(),
				OffsetDateTime.now());

		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of(pendingGoogle, pendingOutlook));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(googleAccount));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, "OUTLOOK"))
				.thenReturn(Optional.of(outlookAccount));
		when(provider.createEvent(eq(CALENDAR_ID), any(), any())).thenReturn("google_event_1");
		when(outlookProvider.createEvent(any(), any(), any())).thenReturn("outlook_event_1");

		service.processPendingSyncs();

		verify(provider).createEvent(any(), any(), any());
		verify(outlookProvider).createEvent(any(), any(), any());
	}

	@Test
	@DisplayName("retrySync reintenta registros FAILED")
	void retrySyncRetriesFailedRecords() {
		mockEventData();
		BookingCalendarSyncRecord failed = failedRecord("CREATE");
		when(syncRepository.findByBookingAndBusiness(BOOKING_ID, BUSINESS_ID)).thenReturn(List.of(failed));
		when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(activeAccount()));

		service.retrySync(BOOKING_ID, BUSINESS_ID);

		verify(provider).createEvent(any(), any(), any());
	}

	@Test
	@DisplayName("retrySync con SYNCED no hace nada")
	void retrySyncWithSyncedSkips() {
		BookingCalendarSyncRecord synced = syncedRecord("CREATE");
		when(syncRepository.findByBookingAndBusiness(BOOKING_ID, BUSINESS_ID)).thenReturn(List.of(synced));
		service.retrySync(BOOKING_ID, BUSINESS_ID);
		verify(provider, never()).createEvent(any(), any(), any());
	}

	@Test
	@DisplayName("getSyncStatus retorna desde el repositorio")
	void getSyncStatusReturnsFromRepository() {
		when(syncRepository.findByBookingAndBusiness(BOOKING_ID, BUSINESS_ID)).thenReturn(List.of(failedRecord()));
		var result = service.getSyncStatus(BOOKING_ID, BUSINESS_ID);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().syncStatus()).isEqualTo("FAILED");
	}

	@Test
	@DisplayName("hasActiveIntegration retorna true cuando existen cuentas activas")
	void hasActiveIntegrationReturnsTrue() {
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		assertThat(service.hasActiveIntegration(BUSINESS_ID)).isTrue();
	}

	@Test
	@DisplayName("hasActiveIntegration retorna false cuando no hay cuentas")
	void hasActiveIntegrationReturnsFalse() {
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of());
		assertThat(service.hasActiveIntegration(BUSINESS_ID)).isFalse();
	}

	@Test
	@DisplayName("processPendingSyncs con sin eventos pendientes no hace nada")
	void processPendingSyncsWithNoPendingDoesNothing() {
		when(syncRepository.findPendingSyncs(50)).thenReturn(List.of());
		service.processPendingSyncs();
		verify(provider, never()).createEvent(any(), any(), any());
	}

	@Test
	@DisplayName("syncConfirmed con FAILED+PENDING existente crea nuevo PENDING")
	void syncConfirmedWithExistingFailedCreatesPending() {
		mockEventData();
		BookingCalendarSyncRecord failed = failedRecord("CREATE");
		when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of(activeAccount()));
		when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
				.thenReturn(Optional.of(failed));

		service.syncConfirmed(BOOKING_ID, BUSINESS_ID);

		verify(syncRepository).insert(any());
	}

	private CalendarIntegrationAccountRecord activeAccount() {
		return new CalendarIntegrationAccountRecord(UUID.randomUUID(), BUSINESS_ID, PROVIDER_NAME, "test@demo.cl",
				"encrypted_access", "encrypted_refresh", OffsetDateTime.now().plusHours(1), CALENDAR_ID, "Calendar",
				true, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), null, OffsetDateTime.now(),
				false);
	}

	private CalendarIntegrationAccountRecord activeAccountWithRefresh() {
		return new CalendarIntegrationAccountRecord(UUID.randomUUID(), BUSINESS_ID, PROVIDER_NAME, "test@demo.cl",
				"encrypted_access", "encrypted_refresh", OffsetDateTime.now().minusHours(1), CALENDAR_ID, "Calendar",
				true, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), null, OffsetDateTime.now(),
				false);
	}

	private BookingCalendarSyncRecord syncedRecord(String action) {
		return new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME, "ext_event_1",
				"SYNCED", action, null, 0, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(),
				OffsetDateTime.now());
	}

	private BookingCalendarSyncRecord pendingRecord(String action) {
		return new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME, null, "PENDING",
				action, null, 0, null, null, OffsetDateTime.now(), OffsetDateTime.now());
	}

	private BookingCalendarSyncRecord pendingRecord(String action, String externalEventId) {
		return new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME, externalEventId,
				"PENDING", action, null, 0, null, null, OffsetDateTime.now(), OffsetDateTime.now());
	}

	private BookingCalendarSyncRecord failedRecord(String action) {
		return new BookingCalendarSyncRecord(UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME, "ext_event_1",
				"FAILED", action, "Error simulado", 1, OffsetDateTime.now(), null, OffsetDateTime.now(),
				OffsetDateTime.now());
	}

	private BookingCalendarSyncRecord failedRecord() {
		return failedRecord("CREATE");
	}
}
