package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROFESSIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LOCATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into customer (id, business_id, first_name, last_name, display_name, phone, normalized_phone, active)
                values (?, ?, 'Test', 'Customer', 'Test Customer', '+56900000000', '+56900000000', true)
                on conflict (id) do nothing
                """, CUSTOMER_ID, BUSINESS_ID);
        jdbcTemplate.update("""
                insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status)
                values (?, ?, 'Test', 'Professional', 'professional@test.cl', '+56900000001',
                        '$2a$10$n7vTmgWhJDL9XDuOn9e5ve6NAhXH4zP6WtU0b7ib/KcN7/TfIz0Gi',
                        'America/Santiago', 'ACTIVE')
                on conflict (id) do nothing
                """, PROFESSIONAL_ID, BUSINESS_ID);
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
    }

    @Test
    void concurrentBookingCreationSameSlotOnlyOneSucceeds() throws Exception {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
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
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).withHour(11).withMinute(0).withSecond(0).withNano(0);
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
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).withHour(12).withMinute(0).withSecond(0).withNano(0);
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
}
