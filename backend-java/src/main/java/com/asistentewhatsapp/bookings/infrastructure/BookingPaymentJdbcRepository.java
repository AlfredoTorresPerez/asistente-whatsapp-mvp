package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingPaymentJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingPaymentJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BookingPaymentBookingRecord findBookingForUpdate(UUID businessId, UUID bookingId) {
        List<BookingPaymentBookingRecord> items = jdbcTemplate.query(
                """
                        select id as booking_id, business_id, status as booking_status,
                               requires_deposit, coalesce(deposit_amount, 0) as deposit_amount, payment_status
                        from booking
                        where business_id = :businessId and id = :bookingId
                        for update
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("bookingId", bookingId),
                bookingRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la reserva asociada al pago.");
        }
        return items.getFirst();
    }

    public BookingPaymentNotificationRecord findNotificationContext(UUID businessId, UUID bookingId) {
        List<BookingPaymentNotificationRecord> items = jdbcTemplate.query(
                """
                        select b.id as booking_id, b.business_id, b.subject, b.status as booking_status,
                               b.starts_at, b.duration_minutes, b.location, bl.name as location_name,
                               s.name as service_name, p.full_name as professional_name, r.name as room_name,
                               c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
                        from booking b
                        join customer c
                          on c.id = b.customer_id
                         and c.business_id = b.business_id
                        left join business_location bl
                          on bl.id = b.location_id
                         and bl.business_id = b.business_id
                        left join aesthetic_service s
                          on s.id = b.service_id
                         and s.business_id = b.business_id
                        left join aesthetic_professional p
                          on p.id = b.professional_id
                         and p.business_id = b.business_id
                        left join agenda_room r
                          on r.id = b.room_id
                         and r.business_id = b.business_id
                        where b.business_id = :businessId
                          and b.id = :bookingId
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
                (resultSet, rowNum) -> new BookingPaymentNotificationRecord(
                        resultSet.getObject("booking_id", UUID.class),
                        resultSet.getObject("business_id", UUID.class),
                        resultSet.getString("subject"),
                        resultSet.getString("booking_status"),
                        resultSet.getObject("starts_at", OffsetDateTime.class),
                        resultSet.getInt("duration_minutes"),
                        resultSet.getString("location"),
                        resultSet.getString("location_name"),
                        resultSet.getString("service_name"),
                        resultSet.getString("professional_name"),
                        resultSet.getString("room_name"),
                        resultSet.getString("customer_name"),
                        resultSet.getString("customer_phone"),
                        resultSet.getString("customer_email")));
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la reserva asociada al pago.");
        }
        return items.getFirst();
    }

    public Optional<BookingPaymentRecord> findExisting(
            UUID businessId,
            String provider,
            String providerPaymentId,
            String idempotencyKey) {
        StringBuilder sql = new StringBuilder(
                """
                        select id, business_id, booking_id, provider, provider_payment_id,
                               idempotency_key, amount, currency, status, approved_at,
                               rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                               manual, created_at
                        from booking_payment
                        where business_id = :businessId
                          and (
                        """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId);
        boolean hasProviderPaymentId = providerPaymentId != null && !providerPaymentId.isBlank();
        boolean hasIdempotencyKey = idempotencyKey != null && !idempotencyKey.isBlank();
        if (hasProviderPaymentId) {
            sql.append("(provider = :provider and provider_payment_id = :providerPaymentId)");
            params.addValue("provider", provider).addValue("providerPaymentId", providerPaymentId);
        }
        if (hasIdempotencyKey) {
            if (hasProviderPaymentId) {
                sql.append(" or ");
            }
            sql.append("idempotency_key = :idempotencyKey");
            params.addValue("idempotencyKey", idempotencyKey);
        }
        if (!hasProviderPaymentId && !hasIdempotencyKey) {
            return Optional.empty();
        }
        sql.append(") order by created_at asc limit 1");
        return jdbcTemplate.query(sql.toString(), params, paymentRowMapper()).stream().findFirst();
    }

    public BookingPaymentRecord insertPayment(
            UUID businessId,
            UUID bookingId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            String rawPayload,
            String metadata,
            OffsetDateTime occurredAt) {
        UUID paymentId = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    """
                            insert into booking_payment (
                                id, business_id, booking_id, provider, provider_payment_id, idempotency_key,
                                amount, currency, status, raw_payload, metadata,
                                approved_at, rejected_at, expired_at, refunded_at
                            ) values (
                                :id, :businessId, :bookingId, :provider, :providerPaymentId, :idempotencyKey,
                                :amount, :currency, :status, cast(:rawPayload as jsonb), cast(:metadata as jsonb),
                                :approvedAt, :rejectedAt, :expiredAt, :refundedAt
                            )
                            """,
                    paymentParams(paymentId, businessId, bookingId, provider, providerPaymentId, idempotencyKey,
                            amount, currency, status, rawPayload, metadata, occurredAt));
        } catch (DuplicateKeyException exception) {
            return findExisting(businessId, provider, providerPaymentId, idempotencyKey)
                    .orElseThrow(() -> exception);
        }
        return findById(paymentId);
    }

    public BookingPaymentRecord insertManualPayment(
            UUID businessId,
            UUID bookingId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            String rawPayload,
            String metadata,
            OffsetDateTime occurredAt) {
        UUID paymentId = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    """
                            insert into booking_payment (
                                id, business_id, booking_id, provider, provider_payment_id, idempotency_key,
                                amount, currency, status, raw_payload, metadata,
                                approved_at, rejected_at, expired_at, refunded_at, manual
                            ) values (
                                :id, :businessId, :bookingId, :provider, :providerPaymentId, :idempotencyKey,
                                :amount, :currency, :status, cast(:rawPayload as jsonb), cast(:metadata as jsonb),
                                :approvedAt, :rejectedAt, :expiredAt, :refundedAt, true
                            )
                            """,
                    paymentParams(paymentId, businessId, bookingId, provider, providerPaymentId, idempotencyKey,
                            amount, currency, status, rawPayload, metadata, occurredAt));
        } catch (DuplicateKeyException exception) {
            return findExisting(businessId, provider, providerPaymentId, idempotencyKey)
                    .orElseThrow(() -> exception);
        }
        return findById(paymentId);
    }

    public BookingPaymentRecord insertCheckoutPayment(
            UUID businessId,
            UUID bookingId,
            String provider,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String checkoutUrl,
            OffsetDateTime checkoutExpiresAt,
            String metadata) {
        UUID paymentId = UUID.randomUUID();
        String resolvedCheckoutUrl = checkoutUrl.contains("{paymentId}")
                ? checkoutUrl
                        .replace("{paymentId}", paymentId.toString())
                        .replace("{bookingId}", bookingId.toString())
                        .replace("{businessId}", businessId.toString())
                : checkoutUrl.endsWith("/")
                ? checkoutUrl + paymentId
                : checkoutUrl + "/" + paymentId;
        try {
            jdbcTemplate.update(
                    """
                            insert into booking_payment (
                                id, business_id, booking_id, provider, idempotency_key,
                                amount, currency, status, raw_payload, metadata,
                                checkout_url, checkout_expires_at, manual
                            ) values (
                                :id, :businessId, :bookingId, :provider, :idempotencyKey,
                                :amount, :currency, 'PENDING', '{}'::jsonb, cast(:metadata as jsonb),
                                :checkoutUrl, :checkoutExpiresAt, false
                            )
                            """,
                    new MapSqlParameterSource()
                            .addValue("id", paymentId)
                            .addValue("businessId", businessId)
                            .addValue("bookingId", bookingId)
                            .addValue("provider", provider)
                            .addValue("idempotencyKey", idempotencyKey)
                            .addValue("amount", amount)
                            .addValue("currency", currency)
                            .addValue("metadata", metadata)
                            .addValue("checkoutUrl", resolvedCheckoutUrl)
                            .addValue("checkoutExpiresAt", checkoutExpiresAt));
        } catch (DuplicateKeyException exception) {
            return findExisting(businessId, provider, null, idempotencyKey).orElseThrow(() -> exception);
        }
        return findById(paymentId);
    }

    public Optional<BookingPaymentRecord> findActiveCheckout(UUID businessId, UUID bookingId, OffsetDateTime now) {
        return jdbcTemplate.query(
                        """
                                select id, business_id, booking_id, provider, provider_payment_id,
                                       idempotency_key, amount, currency, status, approved_at,
                                       rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                                       manual, created_at
                                from booking_payment
                                where business_id = :businessId
                                  and booking_id = :bookingId
                                  and status = 'PENDING'
                                  and checkout_url is not null
                                  and checkout_expires_at > :now
                                order by checkout_expires_at desc, created_at desc
                                limit 1
                                """,
                        new MapSqlParameterSource()
                                .addValue("businessId", businessId)
                                .addValue("bookingId", bookingId)
                                .addValue("now", now),
                        paymentRowMapper())
                .stream()
                .findFirst();
    }

    public List<BookingPaymentRecord> findPayments(UUID businessId, UUID bookingId) {
        return jdbcTemplate.query(
                """
                        select id, business_id, booking_id, provider, provider_payment_id,
                               idempotency_key, amount, currency, status, approved_at,
                               rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                               manual, created_at
                        from booking_payment
                        where business_id = :businessId
                          and booking_id = :bookingId
                        order by created_at desc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
                paymentRowMapper());
    }

    public List<BookingPaymentRecord> findExpiredPendingCheckouts(OffsetDateTime now, int limit) {
        return jdbcTemplate.query(
                """
                        select id, business_id, booking_id, provider, provider_payment_id,
                               idempotency_key, amount, currency, status, approved_at,
                               rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                               manual, created_at
                        from booking_payment
                        where status = 'PENDING'
                          and checkout_url is not null
                          and checkout_expires_at <= :now
                        order by checkout_expires_at asc, created_at asc
                        limit :limit
                        """,
                new MapSqlParameterSource()
                        .addValue("now", now)
                        .addValue("limit", Math.max(1, limit)),
                paymentRowMapper());
    }

    public BookingPaymentRecord updatePaymentStatus(
            UUID paymentId,
            String status,
            String rawPayload,
            String metadata,
            OffsetDateTime occurredAt) {
        jdbcTemplate.update(
                """
                        update booking_payment
                        set status = :status,
                            raw_payload = cast(:rawPayload as jsonb),
                            metadata = cast(:metadata as jsonb),
                            approved_at = case when :status = 'APPROVED' then coalesce(approved_at, :occurredAt) else approved_at end,
                            rejected_at = case when :status = 'REJECTED' then coalesce(rejected_at, :occurredAt) else rejected_at end,
                            expired_at = case when :status = 'EXPIRED' then coalesce(expired_at, :occurredAt) else expired_at end,
                            refunded_at = case when :status = 'REFUNDED' then coalesce(refunded_at, :occurredAt) else refunded_at end,
                            updated_at = current_timestamp
                        where id = :paymentId
                        """,
                new MapSqlParameterSource()
                        .addValue("paymentId", paymentId)
                        .addValue("status", status)
                        .addValue("rawPayload", rawPayload)
                        .addValue("metadata", metadata)
                        .addValue("occurredAt", occurredAt));
        return findById(paymentId);
    }

    public BookingPaymentRecord findById(UUID paymentId) {
        List<BookingPaymentRecord> items = jdbcTemplate.query(
                """
                        select id, business_id, booking_id, provider, provider_payment_id,
                               idempotency_key, amount, currency, status, approved_at,
                               rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                               manual, created_at
                        from booking_payment
                        where id = :paymentId
                        """,
                new MapSqlParameterSource().addValue("paymentId", paymentId),
                paymentRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro el pago de reserva.");
        }
        return items.getFirst();
    }

    public BookingPaymentRecord findByIdForBooking(UUID businessId, UUID bookingId, UUID paymentId) {
        List<BookingPaymentRecord> items = jdbcTemplate.query(
                """
                        select id, business_id, booking_id, provider, provider_payment_id,
                               idempotency_key, amount, currency, status, approved_at,
                               rejected_at, expired_at, refunded_at, checkout_url, checkout_expires_at,
                               manual, created_at
                        from booking_payment
                        where business_id = :businessId
                          and booking_id = :bookingId
                          and id = :paymentId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("bookingId", bookingId)
                        .addValue("paymentId", paymentId),
                paymentRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro el pago de reserva.");
        }
        return items.getFirst();
    }

    public void recalculateBookingPaymentStatus(UUID businessId, UUID bookingId) {
        jdbcTemplate.update(
                """
                        update booking b
                        set payment_status = case
                                when b.requires_deposit = false then 'NOT_REQUIRED'
                                when coalesce(approved.total_approved, 0) >= coalesce(b.deposit_amount, 0) then 'PAID'
                                when coalesce(approved.total_approved, 0) > 0 then 'PARTIAL'
                                when exists (
                                    select 1 from booking_payment p
                                    where p.business_id = b.business_id
                                      and p.booking_id = b.id
                                      and p.status in ('REJECTED', 'EXPIRED')
                                ) then 'FAILED'
                                else 'PENDING'
                            end,
                            updated_at = current_timestamp
                        from (
                            select business_id, booking_id, coalesce(sum(amount), 0) as total_approved
                            from booking_payment
                            where business_id = :businessId
                              and booking_id = :bookingId
                              and status = 'APPROVED'
                            group by business_id, booking_id
                        ) approved
                        where b.business_id = :businessId
                          and b.id = :bookingId
                          and approved.business_id = b.business_id
                          and approved.booking_id = b.id
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
        jdbcTemplate.update(
                """
                        update booking b
                        set payment_status = case
                                when b.requires_deposit = false then 'NOT_REQUIRED'
                                when exists (
                                    select 1 from booking_payment p
                                    where p.business_id = b.business_id
                                      and p.booking_id = b.id
                                      and p.status in ('REJECTED', 'EXPIRED')
                                ) then 'FAILED'
                                else 'PENDING'
                            end,
                            updated_at = current_timestamp
                        where b.business_id = :businessId
                          and b.id = :bookingId
                          and not exists (
                              select 1 from booking_payment p
                              where p.business_id = b.business_id
                                and p.booking_id = b.id
                                and p.status = 'APPROVED'
                          )
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
    }

    public boolean hasApprovedRequiredDeposit(UUID businessId, UUID bookingId) {
        Boolean approved = jdbcTemplate.queryForObject(
                """
                        select case
                            when b.requires_deposit = false then true
                            when coalesce(sum(case when p.status = 'APPROVED' then p.amount else 0 end), 0) >= coalesce(b.deposit_amount, 0) then true
                            else false
                        end
                        from booking b
                        left join booking_payment p
                          on p.business_id = b.business_id
                         and p.booking_id = b.id
                        where b.business_id = :businessId
                          and b.id = :bookingId
                        group by b.requires_deposit, b.deposit_amount
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
                Boolean.class);
        return Boolean.TRUE.equals(approved);
    }

    private MapSqlParameterSource paymentParams(
            UUID paymentId,
            UUID businessId,
            UUID bookingId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            String rawPayload,
            String metadata,
            OffsetDateTime occurredAt) {
        return new MapSqlParameterSource()
                .addValue("id", paymentId)
                .addValue("businessId", businessId)
                .addValue("bookingId", bookingId)
                .addValue("provider", provider)
                .addValue("providerPaymentId", providerPaymentId)
                .addValue("idempotencyKey", idempotencyKey)
                .addValue("amount", amount)
                .addValue("currency", currency)
                .addValue("status", status)
                .addValue("rawPayload", rawPayload)
                .addValue("metadata", metadata)
                .addValue("approvedAt", "APPROVED".equals(status) ? occurredAt : null)
                .addValue("rejectedAt", "REJECTED".equals(status) ? occurredAt : null)
                .addValue("expiredAt", "EXPIRED".equals(status) ? occurredAt : null)
                .addValue("refundedAt", "REFUNDED".equals(status) ? occurredAt : null);
    }

    private RowMapper<BookingPaymentBookingRecord> bookingRowMapper() {
        return (resultSet, rowNum) -> new BookingPaymentBookingRecord(
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("business_id", UUID.class),
                resultSet.getString("booking_status"),
                resultSet.getBoolean("requires_deposit"),
                resultSet.getBigDecimal("deposit_amount"),
                resultSet.getString("payment_status"));
    }

    private RowMapper<BookingPaymentRecord> paymentRowMapper() {
        return (resultSet, rowNum) -> new BookingPaymentRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("business_id", UUID.class),
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getString("provider"),
                resultSet.getString("provider_payment_id"),
                resultSet.getString("idempotency_key"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                resultSet.getObject("approved_at", OffsetDateTime.class),
                resultSet.getObject("rejected_at", OffsetDateTime.class),
                resultSet.getObject("expired_at", OffsetDateTime.class),
                resultSet.getObject("refunded_at", OffsetDateTime.class),
                resultSet.getString("checkout_url"),
                resultSet.getObject("checkout_expires_at", OffsetDateTime.class),
                resultSet.getBoolean("manual"),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    public record BookingPaymentBookingRecord(
            UUID bookingId,
            UUID businessId,
            String bookingStatus,
            boolean requiresDeposit,
            BigDecimal depositAmount,
            String paymentStatus) {
    }

    public record BookingPaymentNotificationRecord(
            UUID bookingId,
            UUID businessId,
            String subject,
            String bookingStatus,
            OffsetDateTime startsAt,
            int durationMinutes,
            String location,
            String locationName,
            String serviceName,
            String professionalName,
            String roomName,
            String customerName,
            String customerPhone,
            String customerEmail) {
    }

    public record BookingPaymentRecord(
            UUID id,
            UUID businessId,
            UUID bookingId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            OffsetDateTime approvedAt,
            OffsetDateTime rejectedAt,
            OffsetDateTime expiredAt,
            OffsetDateTime refundedAt,
            String checkoutUrl,
            OffsetDateTime checkoutExpiresAt,
            boolean manual,
            OffsetDateTime createdAt) {
    }
}
