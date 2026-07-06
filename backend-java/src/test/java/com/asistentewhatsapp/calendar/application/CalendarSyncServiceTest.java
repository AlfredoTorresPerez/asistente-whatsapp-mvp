package com.asistentewhatsapp.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.calendar.infrastructure.TokenEncryptionService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CalendarSyncServiceTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String PROVIDER_NAME = "GOOGLE";

    private BookingCalendarSyncJdbcRepository syncRepository;
    private CalendarIntegrationJdbcRepository accountRepository;
    private CalendarProvider provider;
    private CalendarSyncService service;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        syncRepository = mock(BookingCalendarSyncJdbcRepository.class);
        accountRepository = mock(CalendarIntegrationJdbcRepository.class);
        TokenEncryptionService tokenEncryption = mock(TokenEncryptionService.class);
        provider = mock(CalendarProvider.class);
        when(provider.getProviderName()).thenReturn(PROVIDER_NAME);
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        service = new CalendarSyncService(syncRepository, accountRepository, tokenEncryption,
                List.of(provider), jdbcTemplate);
    }

    private void mockEventData() {
        when(jdbcTemplate.queryForObject(anyString(), any(java.util.Map.class), any(RowMapper.class)))
                .thenReturn(new CalendarEventData("Test", "Desc", "Loc",
                        OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1),
                        "America/Santiago", "test@demo.cl", "Test"));
    }

    @Test
    void syncConfirmedWhenNoAccountsDoesNothing() {
        mockEventData();
        when(accountRepository.findActiveByBusiness(BUSINESS_ID)).thenReturn(List.of());
        service.syncConfirmed(BOOKING_ID, BUSINESS_ID);
        verify(syncRepository, never()).insert(any());
    }

    @Test
    void syncConfirmedWithAccountCreatesPendingAndSyncs() {
        mockEventData();
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of(activeAccount()));
        when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
                .thenReturn(Optional.empty());
        service.syncConfirmed(BOOKING_ID, BUSINESS_ID);
        verify(syncRepository).insert(any());
        verify(provider).createEvent(any(), any());
    }

    @Test
    void syncRescheduledWithExistingSyncedAndDifferentActionDoesNothing() {
        mockEventData();
        BookingCalendarSyncRecord synced = syncedRecord("UPDATE");
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of(activeAccount()));
        when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
                .thenReturn(Optional.of(synced));
        service.syncRescheduled(BOOKING_ID, BUSINESS_ID);
        verify(provider, never()).updateEvent(any(), any(), any());
    }

    @Test
    void syncRescheduledWithFailedRetries() {
        mockEventData();
        BookingCalendarSyncRecord failed = failedRecord("UPDATE");
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of(activeAccount()));
        when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
                .thenReturn(Optional.of(failed));
        service.syncRescheduled(BOOKING_ID, BUSINESS_ID);
        verify(provider).updateEvent(eq("ext_event_1"), any(), any());
    }

    @Test
    void syncCancelledWithExistingSyncedDeletesEvent() {
        mockEventData();
        BookingCalendarSyncRecord synced = syncedRecord("DELETE");
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of(activeAccount()));
        when(syncRepository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER_NAME))
                .thenReturn(Optional.of(synced));
        service.syncCancelled(BOOKING_ID, BUSINESS_ID);
        verify(provider).deleteEvent(eq("ext_event_1"), any());
    }

    @Test
    void retrySyncWithFailedRecordRetries() {
        mockEventData();
        BookingCalendarSyncRecord failed = failedRecord();
        when(syncRepository.findByBooking(BOOKING_ID))
                .thenReturn(List.of(failed));
        when(accountRepository.findByBusinessAndProvider(BUSINESS_ID, PROVIDER_NAME))
                .thenReturn(Optional.of(activeAccount()));
        service.retrySync(BOOKING_ID);
        verify(provider).deleteEvent(any(), any());
    }

    @Test
    void retrySyncWithNonFailedSkips() {
        BookingCalendarSyncRecord synced = syncedRecord("CREATE");
        when(syncRepository.findByBooking(BOOKING_ID))
                .thenReturn(List.of(synced));
        service.retrySync(BOOKING_ID);
        verify(provider, never()).createEvent(any(), any());
    }

    @Test
    void getSyncStatusReturnsFromRepository() {
        when(syncRepository.findByBooking(BOOKING_ID))
                .thenReturn(List.of(failedRecord()));
        List<BookingCalendarSyncRecord> result = service.getSyncStatus(BOOKING_ID);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().syncStatus()).isEqualTo("FAILED");
    }

    @Test
    void hasActiveIntegrationReturnsTrueWhenAccountsExist() {
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of(activeAccount()));
        assertThat(service.hasActiveIntegration(BUSINESS_ID)).isTrue();
    }

    @Test
    void hasActiveIntegrationReturnsFalseWhenNoAccounts() {
        when(accountRepository.findActiveByBusiness(BUSINESS_ID))
                .thenReturn(List.of());
        assertThat(service.hasActiveIntegration(BUSINESS_ID)).isFalse();
    }

    private CalendarIntegrationAccountRecord activeAccount() {
        return new CalendarIntegrationAccountRecord(
                UUID.randomUUID(), BUSINESS_ID, PROVIDER_NAME, "test@demo.cl",
                "encrypted_access", "encrypted_refresh",
                OffsetDateTime.now().plusHours(1), "primary", "Calendar",
                true, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private BookingCalendarSyncRecord syncedRecord(String action) {
        return new BookingCalendarSyncRecord(
                UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME,
                "ext_event_1", "SYNCED", action, null,
                0, OffsetDateTime.now(), OffsetDateTime.now(),
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private BookingCalendarSyncRecord failedRecord() {
        return failedRecord("DELETE");
    }

    private BookingCalendarSyncRecord failedRecord(String action) {
        return new BookingCalendarSyncRecord(
                UUID.randomUUID(), BOOKING_ID, BUSINESS_ID, PROVIDER_NAME,
                "ext_event_1", "FAILED", action, "Error simulado",
                1, OffsetDateTime.now(), null,
                OffsetDateTime.now(), OffsetDateTime.now());
    }
}
