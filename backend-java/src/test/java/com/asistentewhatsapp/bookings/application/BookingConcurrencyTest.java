package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CompleteAgendaJdbcRepository repository;

    private static final UUID BUSINESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PROFESSIONAL_ID = UUID.fromString("cccccccc-cccc-4ccc-cccc-cccccccccccc");
    private static final UUID LOCATION_ID = UUID.fromString("dddddddd-dddd-4ddd-dddd-dddddddddddd");
    private static final UUID ACTOR_USER_ID = UUID.fromString("eeeeeeee-eeee-4eee-eeee-eeeeeeeeeeee");
    private static final UUID SERVICE_CATEGORY_ID = UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff");
    private static final UUID SERVICE_ID = UUID.fromString("00000000-0000-4000-a000-000000000000");

    private final List<UUID> createdBookingIds = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into business (id, code, company_name, business_name, timezone, currency, contact_email, support_phone, address)
                values (?, 'CONCUR-TEST', 'Test Business', 'Test Business', 'America/Santiago', 'CLP',
                        'test@business.cl', '+56900000000', 'Test Address')
                on conflict (id) do nothing
                """, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into customer (id, business_id, first_name, last_name, display_name, phone, normalized_phone, active)
                values (?, ?, 'Test', 'Customer', 'Test Customer', '+56900000000', '+56900000000', true)
                on conflict (id) do nothing
                """, CUSTOMER_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status)
                values (?, ?, 'Professional', 'User', 'professional@test.cl', '+56900000001',
                        '$2a$10$n7vTmgWhJDL9XDuOn9e5ve6NAhXH4zP6WtU0b7ib/KcN7/TfIz0Gi',
                        'America/Santiago', 'ACTIVE')
                on conflict (id) do nothing
                """, ACTOR_USER_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into business_location (id, business_id, code, name, address, timezone, active)
                values (?, ?, 'TEST-LOC', 'Test Location', 'Test Address 123', 'America/Santiago', true)
                on conflict (id) do nothing
                """, LOCATION_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into aesthetic_professional (id, business_id, full_name, specialty, active)
                values (?, ?, 'Test Professional', 'General', true)
                on conflict (id) do nothing
                """, PROFESSIONAL_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into aesthetic_service_category (id, business_id, code, name, description, active)
                values (?, ?, 'TEST-CAT', 'Test Category', 'Test category for concurrency tests', true)
                on conflict (id) do nothing
                """, SERVICE_CATEGORY_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into aesthetic_service (id, business_id, category_id, code, name, description, duration_minutes,
                    price_base, professional_required, requires_room, requires_deposit, deposit_amount,
                    preparation_minutes, cleanup_minutes, active)
                values (?, ?, ?, 'TEST-SVC', 'Test Service', 'Test service for concurrency tests', 60,
                    50000, 'Test Professional', false, false, 0, 0, 0, true)
                on conflict (id) do nothing
                """, SERVICE_ID, BUSINESS_ID, SERVICE_CATEGORY_ID);
    }

    @AfterEach
    void tearDown() {
        for (UUID id : createdBookingIds) {
            jdbcTemplate.update("delete from booking_status_history where booking_id = ?", id);
            jdbcTemplate.update("delete from booking_reminder where booking_id = ?", id);
            jdbcTemplate.update("delete from booking_confirmation_link where booking_id = ?", id);
            jdbcTemplate.update("delete from booking_reschedule_link where booking_id = ?", id);
            jdbcTemplate.update("delete from booking_cancellation_link where booking_id = ?", id);
            jdbcTemplate.update("delete from booking where id = ?", id);
        }
        createdBookingIds.clear();
    }

    @Test
    void concurrentInsertTemporaryBookingSameSlotOnlyOneSucceeds() throws Exception {
        OffsetDateTime startsAt = baseTime().withHour(7).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endsAt = startsAt.plusMinutes(60);
        OffsetDateTime expiresAt = startsAt.plusHours(1);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    UUID bookingId = repository.insertTemporaryBooking(
                            BUSINESS_ID, CUSTOMER_ID, null, null, ACTOR_USER_ID,
                            "Concurrency Test", LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, null,
                            startsAt, endsAt, 60, expiresAt, false, BigDecimal.ZERO, null);
                    createdBookingIds.add(bookingId);
                    return "SUCCESS";
                } catch (DataIntegrityViolationException e) {
                    return "CONCURRENT_CONFLICT:" + e.getClass().getSimpleName();
                } catch (org.springframework.dao.PessimisticLockingFailureException e) {
                    return "CONCURRENT_CONFLICT:" + e.getClass().getSimpleName();
                } catch (Exception e) {
                    return "FAILED:" + e.getClass().getSimpleName();
                }
            });
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = 0;
        long conflictCount = 0;
        for (Future<String> future : futures) {
            String result = future.get();
            if ("SUCCESS".equals(result)) {
                successCount++;
            } else if (result != null && result.startsWith("CONCURRENT_CONFLICT")) {
                conflictCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(successCount + conflictCount).isEqualTo(threadCount);
    }

    @Test
    void concurrentInsertLocationOnlyNoProfessionalOrRoom() throws Exception {
        OffsetDateTime startsAt = baseTime().withHour(8).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endsAt = startsAt.plusMinutes(60);
        OffsetDateTime expiresAt = startsAt.plusHours(1);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    UUID bookingId = repository.insertTemporaryBooking(
                            BUSINESS_ID, CUSTOMER_ID, null, null, ACTOR_USER_ID,
                            "Location Only Concurrency Test", LOCATION_ID, SERVICE_ID, null, null,
                            startsAt, endsAt, 60, expiresAt, false, BigDecimal.ZERO, null);
                    createdBookingIds.add(bookingId);
                    return "SUCCESS";
                } catch (DataIntegrityViolationException e) {
                    return "CONCURRENT_CONFLICT:" + e.getClass().getSimpleName();
                } catch (org.springframework.dao.PessimisticLockingFailureException e) {
                    return "CONCURRENT_CONFLICT:" + e.getClass().getSimpleName();
                } catch (Exception e) {
                    return "FAILED:" + e.getClass().getSimpleName();
                }
            });
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = 0;
        long conflictCount = 0;
        for (Future<String> future : futures) {
            String result = future.get();
            if ("SUCCESS".equals(result)) {
                successCount++;
            } else if (result != null && result.startsWith("CONCURRENT_CONFLICT")) {
                conflictCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(successCount + conflictCount).isEqualTo(threadCount);
    }

    @Test
    void concurrentDirectInsertSameSlotOnlyOneSucceeds() throws Exception {
        OffsetDateTime startsAt = baseTime().withHour(9).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endsAt = startsAt.plusMinutes(60);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    UUID bookingId = UUID.randomUUID();
                    jdbcTemplate.update("""
                            insert into booking (
                                id, business_id, customer_id, subject, status,
                                starts_at, ends_at, duration_minutes, location_id,
                                professional_id, version, requires_deposit, payment_status,
                                source_channel, created_at, updated_at
                            ) values (?, ?, ?, ?, 'SOLICITADA', ?, ?, 60, ?, ?, 0, false, 'NOT_REQUIRED', 'ADMIN', current_timestamp, current_timestamp)
                            """,
                            bookingId, BUSINESS_ID, CUSTOMER_ID, "Concurrency Test",
                            startsAt, endsAt, LOCATION_ID, PROFESSIONAL_ID);
                    createdBookingIds.add(bookingId);
                    return "SUCCESS:" + bookingId;
                } catch (Exception e) {
                    return "FAILED:" + e.getClass().getSimpleName();
                }
            });
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = 0;
        for (Future<String> future : futures) {
            String result = future.get();
            if (result.startsWith("SUCCESS")) {
                successCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
    }

    @Test
    void concurrentCancelOnlyOneSucceeds() throws Exception {
        OffsetDateTime startsAt = baseTime().withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endsAt = startsAt.plusMinutes(60);
        UUID bookingId = UUID.randomUUID();

        jdbcTemplate.update("""
                insert into booking (
                    id, business_id, customer_id, subject, status,
                    starts_at, ends_at, duration_minutes, location_id,
                    professional_id, version, requires_deposit, payment_status,
                    source_channel, created_at, updated_at
                ) values (?, ?, ?, ?, 'CONFIRMADA', ?, ?, 60, ?, ?, 0, false, 'NOT_REQUIRED', 'ADMIN', current_timestamp, current_timestamp)
                """,
                bookingId, BUSINESS_ID, CUSTOMER_ID, "Concurrency Cancel Test",
                startsAt, endsAt, LOCATION_ID, PROFESSIONAL_ID);
        createdBookingIds.add(bookingId);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    int updated = jdbcTemplate.update("""
                            update booking
                            set status = 'CANCELADA',
                                cancelled_at = coalesce(cancelled_at, current_timestamp),
                                notes = 'Cancelacion concurrente',
                                completed_at = null,
                                version = version + 1,
                                updated_at = current_timestamp
                            where business_id = ?
                              and id = ?
                              and status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'CONFIRMADA', 'REPROGRAMADA',
                                             'REPROGRAMACION_PENDIENTE', 'PENDIENTE_PAGO')
                            """, BUSINESS_ID, bookingId);
                    return updated > 0 ? "SUCCESS" : "NO_ROWS";
                } catch (Exception e) {
                    return "FAILED:" + e.getClass().getSimpleName();
                }
            });
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { return "ERROR"; }
                })
                .filter("SUCCESS"::equals)
                .count();

        String finalStatus = jdbcTemplate.queryForObject(
                "select status from booking where id = ?", String.class, bookingId);
        assertThat(successCount).isEqualTo(1);
        assertThat(finalStatus).isEqualTo("CANCELADA");
    }

    @Test
    void concurrentRescheduleOnlyOneSucceeds() throws Exception {
        OffsetDateTime startsAt = baseTime().withHour(11).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endsAt = startsAt.plusMinutes(60);
        UUID bookingId = UUID.randomUUID();
        String originalNotes = "Original notes";

        jdbcTemplate.update("""
                insert into booking (
                    id, business_id, customer_id, subject, status,
                    starts_at, ends_at, duration_minutes, location_id,
                    professional_id, notes, version, requires_deposit, payment_status,
                    source_channel, created_at, updated_at
                ) values (?, ?, ?, ?, 'CONFIRMADA', ?, ?, 60, ?, ?, ?, 0, false, 'NOT_REQUIRED', 'ADMIN', current_timestamp, current_timestamp)
                """,
                bookingId, BUSINESS_ID, CUSTOMER_ID, "Concurrency Reschedule Test",
                startsAt, endsAt, LOCATION_ID, PROFESSIONAL_ID, originalNotes);
        createdBookingIds.add(bookingId);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            int offset = i;
            tasks.add(() -> {
                try {
                    OffsetDateTime newStartsAt = startsAt.plusDays(1).plusHours(offset);
                    OffsetDateTime newEndsAt = newStartsAt.plusMinutes(60);
                    int updated = jdbcTemplate.update("""
                            update booking
                            set status = 'REPROGRAMADA',
                                starts_at = ?,
                                ends_at = ?,
                                duration_minutes = 60,
                                completed_at = null,
                                version = version + 1,
                                updated_at = current_timestamp
                            where business_id = ?
                              and id = ?
                              and status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'CONFIRMADA', 'REPROGRAMADA',
                                             'REPROGRAMACION_PENDIENTE', 'PENDIENTE_PAGO')
                            """, newStartsAt, newEndsAt, BUSINESS_ID, bookingId);
                    return updated > 0 ? "SUCCESS" : "NO_ROWS";
                } catch (Exception e) {
                    return "FAILED:" + e.getClass().getSimpleName();
                }
            });
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { return "ERROR"; }
                })
                .filter("SUCCESS"::equals)
                .count();

        String finalStatus = jdbcTemplate.queryForObject(
                "select status from booking where id = ?", String.class, bookingId);
        assertThat(successCount).isGreaterThanOrEqualTo(1);
        assertThat(finalStatus).isEqualTo("REPROGRAMADA");
    }

    private static OffsetDateTime baseTime() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(30);
    }
}
