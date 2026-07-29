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

	// ---- Booking lookup ----

	public BookingPaymentBookingRecord findBookingForUpdate(UUID businessId, UUID bookingId) {
		List<BookingPaymentBookingRecord> items = jdbcTemplate.query(
				"select id as booking_id, business_id, status as booking_status, "
						+ "requires_deposit, coalesce(deposit_amount, 0) as deposit_amount, payment_status "
						+ "from booking where business_id = :businessId and id = :bookingId for update",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				bookingRowMapper());
		if (items.isEmpty())
			throw new ResourceNotFoundException("No se encontro la reserva asociada al pago.");
		return items.getFirst();
	}

	public BookingPaymentNotificationRecord findNotificationContext(UUID businessId, UUID bookingId) {
		List<BookingPaymentNotificationRecord> items = jdbcTemplate.query("""
				select b.id as booking_id, b.business_id, b.subject, b.status as booking_status,
				       b.starts_at, b.duration_minutes, b.location, bl.name as location_name,
				       s.name as service_name, p.full_name as professional_name, r.name as room_name,
				       c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where b.business_id = :businessId and b.id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				(rs, rowNum) -> new BookingPaymentNotificationRecord(rs.getObject("booking_id", UUID.class),
						rs.getObject("business_id", UUID.class), rs.getString("subject"),
						rs.getString("booking_status"), rs.getObject("starts_at", OffsetDateTime.class),
						rs.getInt("duration_minutes"), rs.getString("location"), rs.getString("location_name"),
						rs.getString("service_name"), rs.getString("professional_name"), rs.getString("room_name"),
						rs.getString("customer_name"), rs.getString("customer_phone"), rs.getString("customer_email")));
		if (items.isEmpty())
			throw new ResourceNotFoundException("No se encontro la reserva asociada al pago.");
		return items.getFirst();
	}

	public BigDecimal findServicePrice(UUID businessId, UUID bookingId) {
		List<BigDecimal> items = jdbcTemplate.query("""
				select coalesce(s.price_base, 0) from booking b
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				where b.business_id = :businessId and b.id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				(rs, rowNum) -> rs.getBigDecimal("price_base"));
		return items.isEmpty() ? BigDecimal.ZERO : items.getFirst();
	}

	// ---- Payment lookup ----

	public Optional<BookingPaymentRecord> findExisting(UUID businessId, String provider, String providerPaymentId,
			String providerExternalReference, String idempotencyKey) {
		StringBuilder sql = new StringBuilder("select id, business_id, booking_id, provider, provider_payment_id, "
				+ "provider_preference_id, provider_external_reference, "
				+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
				+ "approved_at, rejected_at, expired_at, refunded_at, "
				+ "checkout_url, checkout_expires_at, manual, created_at "
				+ "from booking_payment where business_id = :businessId and (");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("provider", provider);
		boolean hasProviderPaymentId = providerPaymentId != null && !providerPaymentId.isBlank();
		boolean hasExternalRef = providerExternalReference != null && !providerExternalReference.isBlank();
		boolean hasIdempKey = idempotencyKey != null && !idempotencyKey.isBlank();
		boolean hasCondition = false;
		if (hasProviderPaymentId) {
			sql.append("(provider = :provider and provider_payment_id = :providerPaymentId)");
			params.addValue("providerPaymentId", providerPaymentId);
			hasCondition = true;
		}
		if (hasExternalRef) {
			if (hasCondition)
				sql.append(" or ");
			sql.append("(provider = :provider and provider_external_reference = :externalRef)");
			params.addValue("externalRef", providerExternalReference);
			hasCondition = true;
		}
		if (hasIdempKey) {
			if (hasCondition)
				sql.append(" or ");
			sql.append("idempotency_key = :idempotencyKey");
			params.addValue("idempotencyKey", idempotencyKey);
			hasCondition = true;
		}
		if (!hasCondition)
			return Optional.empty();
		sql.append(") order by created_at asc limit 1");
		return jdbcTemplate.query(sql.toString(), params, paymentRowMapper()).stream().findFirst();
	}

	public BookingPaymentRecord findByProviderExternalReference(String provider, String externalRef) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_external_reference = :externalRef order by created_at desc limit 1",
				new MapSqlParameterSource().addValue("provider", provider).addValue("externalRef", externalRef),
				paymentRowMapper());
		return items.isEmpty() ? null : items.getFirst();
	}

	public BookingPaymentRecord findByProviderPreferenceId(String provider, String preferenceId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_preference_id = :preferenceId order by created_at desc limit 1",
				new MapSqlParameterSource().addValue("provider", provider).addValue("preferenceId", preferenceId),
				paymentRowMapper());
		return items.isEmpty() ? null : items.getFirst();
	}

	public BookingPaymentRecord findByProviderPaymentId(String provider, String providerPaymentId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_payment_id = :providerPaymentId order by created_at desc limit 1",
				new MapSqlParameterSource().addValue("provider", provider).addValue("providerPaymentId",
						providerPaymentId),
				paymentRowMapper());
		return items.isEmpty() ? null : items.getFirst();
	}

	// ---- Insert ----

	public BookingPaymentRecord insertPayment(UUID businessId, UUID bookingId, String provider,
			String providerPaymentId, String providerPreferenceId, String providerExternalReference,
			String idempotencyKey, BigDecimal amount, String currency, String status, String rawPayload,
			String metadata, OffsetDateTime occurredAt, String statusDetail, String rawStatus, String paymentMethodId,
			Integer installments, String payerEmail, String paymentPurpose) {
		UUID paymentId = UUID.randomUUID();
		try {
			jdbcTemplate.update("""
					insert into booking_payment (
					    id, business_id, booking_id, provider,
					    provider_payment_id, provider_preference_id, provider_external_reference,
					    idempotency_key, amount, currency, status,
					    provider_status_detail, provider_raw_status, provider_payment_method,
					    provider_installments, payer_email, payment_purpose,
					    raw_payload, metadata,
					    approved_at, rejected_at, expired_at, refunded_at
					) values (
					    :id, :businessId, :bookingId, :provider,
					    :providerPaymentId, :providerPreferenceId, :providerExternalReference,
					    :idempotencyKey, :amount, :currency, :status,
					    :statusDetail, :rawStatus, :paymentMethodId,
					    :installments, :payerEmail, :paymentPurpose,
					    cast(:rawPayload as jsonb), cast(:metadata as jsonb),
					    :approvedAt, :rejectedAt, :expiredAt, :refundedAt
					)
					""",
					paymentParams(paymentId, businessId, bookingId, provider, providerPaymentId, providerPreferenceId,
							providerExternalReference, idempotencyKey, amount, currency, status, rawPayload, metadata,
							occurredAt, statusDetail, rawStatus, paymentMethodId, installments, payerEmail));
		} catch (DuplicateKeyException e) {
			BookingPaymentRecord existing = findExisting(businessId, provider, providerPaymentId,
					providerExternalReference, idempotencyKey).orElseThrow(() -> e);
			return existing;
		}
		return findById(paymentId);
	}

	public BookingPaymentRecord insertManualPayment(UUID businessId, UUID bookingId, String provider,
			String providerPaymentId, String idempotencyKey, BigDecimal amount, String currency, String status,
			String rawPayload, String metadata, OffsetDateTime occurredAt) {
		UUID paymentId = UUID.randomUUID();
		try {
			jdbcTemplate.update("""
					insert into booking_payment (
					    id, business_id, booking_id, provider, provider_payment_id,
					    idempotency_key, amount, currency, status,
					    payment_purpose,
					    raw_payload, metadata,
					    approved_at, rejected_at, expired_at, refunded_at, manual
					) values (
					    :id, :businessId, :bookingId, :provider, :providerPaymentId,
					    :idempotencyKey, :amount, :currency, :status,
					    'MANUAL',
					    cast(:rawPayload as jsonb), cast(:metadata as jsonb),
					    :approvedAt, :rejectedAt, :expiredAt, :refundedAt, true
					)
					""",
					paymentParams(paymentId, businessId, bookingId, provider, providerPaymentId, null, null,
							idempotencyKey, amount, currency, status, rawPayload, metadata, occurredAt, null, null,
							null, null, null));
		} catch (DuplicateKeyException e) {
			return findExisting(businessId, provider, providerPaymentId, null, idempotencyKey).orElseThrow(() -> e);
		}
		return findById(paymentId);
	}

	public BookingPaymentRecord insertPaymentFromCheckout(UUID businessId, UUID bookingId, String provider,
			String providerName, String providerPaymentId, String providerPreferenceId,
			String providerExternalReference, String idempotencyKey, BigDecimal amount, String currency,
			String paymentPurpose, String checkoutUrl, OffsetDateTime checkoutExpiresAt, String metadata,
			UUID paymentId) {
		String resolvedCheckoutUrl = checkoutUrl.contains("{paymentId}")
				? checkoutUrl.replace("{paymentId}", paymentId.toString()).replace("{bookingId}", bookingId.toString())
						.replace("{businessId}", businessId.toString())
				: checkoutUrl.endsWith("/") ? checkoutUrl + paymentId : checkoutUrl + "/" + paymentId;
		try {
			jdbcTemplate.update("""
					insert into booking_payment (
					    id, business_id, booking_id, provider,
					    provider_payment_id, provider_preference_id, provider_external_reference,
					    idempotency_key, amount, currency, status,
					    payment_purpose,
					    raw_payload, metadata,
					    checkout_url, checkout_expires_at, manual
					) values (
					    :id, :businessId, :bookingId, :provider,
					    :providerPaymentId, :providerPreferenceId, :providerExternalReference,
					    :idempotencyKey, :amount, :currency, 'PENDING',
					    :paymentPurpose,
					    '{}'::jsonb, cast(:metadata as jsonb),
					    :checkoutUrl, :checkoutExpiresAt, false
					)
					""",
					new MapSqlParameterSource().addValue("id", paymentId).addValue("businessId", businessId)
							.addValue("bookingId", bookingId).addValue("provider", provider)
							.addValue("providerPaymentId", providerPaymentId)
							.addValue("providerPreferenceId", providerPreferenceId)
							.addValue("providerExternalReference", providerExternalReference)
							.addValue("idempotencyKey", idempotencyKey).addValue("amount", amount)
							.addValue("currency", currency).addValue("paymentPurpose", paymentPurpose)
							.addValue("metadata", metadata).addValue("checkoutUrl", resolvedCheckoutUrl)
							.addValue("checkoutExpiresAt", checkoutExpiresAt));
		} catch (DuplicateKeyException e) {
			return findExisting(businessId, provider, null, providerExternalReference, idempotencyKey)
					.orElseThrow(() -> e);
		}
		return findById(paymentId);
	}

	// ---- Update ----

	public void updatePaymentProviderId(UUID paymentId, String providerPaymentId) {
		jdbcTemplate.update(
				"update booking_payment set provider_payment_id = :providerPaymentId, updated_at = current_timestamp where id = :paymentId",
				new MapSqlParameterSource().addValue("paymentId", paymentId).addValue("providerPaymentId",
						providerPaymentId));
	}

	public void updatePaymentPreferenceId(UUID paymentId, String preferenceId) {
		jdbcTemplate.update(
				"update booking_payment set provider_preference_id = :preferenceId, updated_at = current_timestamp where id = :paymentId",
				new MapSqlParameterSource().addValue("paymentId", paymentId).addValue("preferenceId", preferenceId));
	}

	public void updatePaymentProviderExternalReference(UUID paymentId, String externalRef) {
		jdbcTemplate.update(
				"update booking_payment set provider_external_reference = :externalRef, updated_at = current_timestamp where id = :paymentId",
				new MapSqlParameterSource().addValue("paymentId", paymentId).addValue("externalRef", externalRef));
	}

	public void updateBookingStatus(UUID businessId, UUID bookingId, String status, String reason, String description) {
		jdbcTemplate.update(
				"update booking set status = :status, status_reason = :reason, status_description = :description, updated_at = current_timestamp "
						+ "where business_id = :businessId and id = :bookingId",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
						.addValue("status", status).addValue("reason", reason).addValue("description", description));
	}

	public BookingPaymentRecord updatePaymentProviderStatus(UUID paymentId, String status, String rawPayload,
			String metadata, OffsetDateTime occurredAt, String statusDetail, String rawStatus, String paymentMethodId,
			Integer installments, String payerEmail) {
		jdbcTemplate.update(
				"""
						update booking_payment
						set status = :status,
						    raw_payload = cast(:rawPayload as jsonb),
						    metadata = cast(:metadata as jsonb),
						    provider_status_detail = coalesce(:statusDetail, provider_status_detail),
						    provider_raw_status = coalesce(:rawStatus, provider_raw_status),
						    provider_payment_method = coalesce(:paymentMethodId, provider_payment_method),
						    provider_installments = coalesce(:installments, provider_installments),
						    payer_email = coalesce(:payerEmail, payer_email),
						    approved_at = case when :status = 'APPROVED' then coalesce(approved_at, :occurredAt) else approved_at end,
						    rejected_at = case when :status = 'REJECTED' then coalesce(rejected_at, :occurredAt) else rejected_at end,
						    expired_at = case when :status = 'EXPIRED' then coalesce(expired_at, :occurredAt) else expired_at end,
						    refunded_at = case when :status = 'REFUNDED' then coalesce(refunded_at, :occurredAt) else refunded_at end,
						    reconciled_at = case when :status in ('APPROVED', 'REJECTED', 'REFUNDED') then coalesce(reconciled_at, current_timestamp) else reconciled_at end,
						    updated_at = current_timestamp
						where id = :paymentId
						""",
				new MapSqlParameterSource().addValue("paymentId", paymentId).addValue("status", status)
						.addValue("rawPayload", rawPayload).addValue("metadata", metadata)
						.addValue("occurredAt", occurredAt).addValue("statusDetail", statusDetail)
						.addValue("rawStatus", rawStatus).addValue("paymentMethodId", paymentMethodId)
						.addValue("installments", installments).addValue("payerEmail", payerEmail));
		return findById(paymentId);
	}

	// ---- Find queries ----

	public Optional<BookingPaymentRecord> findActiveCheckout(UUID businessId, UUID bookingId, OffsetDateTime now) {
		return jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where business_id = :businessId "
						+ "and booking_id = :bookingId and status = 'PENDING' "
						+ "and checkout_url is not null and checkout_expires_at > :now "
						+ "order by checkout_expires_at desc, created_at desc limit 1",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
						.addValue("now", now),
				paymentRowMapper()).stream().findFirst();
	}

	public List<BookingPaymentRecord> findPayments(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where business_id = :businessId and booking_id = :bookingId "
						+ "order by created_at desc",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				paymentRowMapper());
	}

	public List<BookingPaymentRecord> findExpiredPendingCheckouts(OffsetDateTime now, int limit) {
		return jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where status = 'PENDING' "
						+ "and checkout_url is not null and checkout_expires_at <= :now "
						+ "order by checkout_expires_at asc, created_at asc limit :limit",
				new MapSqlParameterSource().addValue("now", now).addValue("limit", Math.max(1, limit)),
				paymentRowMapper());
	}

	public PublicBookingPaymentDetailRecord findPaymentDetail(UUID paymentId) {
		List<PublicBookingPaymentDetailRecord> items = jdbcTemplate.query("""
				select p.id, p.business_id, p.booking_id, p.provider, p.provider_payment_id,
				       p.amount, p.currency, p.status, p.checkout_url, p.checkout_expires_at,
				       p.manual, p.approved_at, p.rejected_at, p.expired_at, p.refunded_at,
				       p.created_at,
				       b.status as booking_status,
				       b.payment_status as booking_payment_status,
				       coalesce(s.name, b.subject) as service_name,
				       b.subject,
				       prof.full_name as professional_name,
				       r.name as room_name,
				       b.starts_at, b.duration_minutes,
				       coalesce(l.name, b.location) as location_name,
				       c.display_name as customer_name
				from booking_payment p
				join booking b on b.id = p.booking_id and b.business_id = p.business_id
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id
				left join aesthetic_professional prof on prof.id = b.professional_id
				left join agenda_room r on r.id = b.room_id
				left join business_location l on l.id = b.location_id
				where p.id = :paymentId
				""", new MapSqlParameterSource().addValue("paymentId", paymentId),
				(rs, rowNum) -> new PublicBookingPaymentDetailRecord(rs.getObject("id", UUID.class),
						rs.getObject("booking_id", UUID.class), rs.getString("provider"),
						rs.getString("provider_payment_id"), rs.getBigDecimal("amount"), rs.getString("currency"),
						rs.getString("status"), rs.getString("checkout_url"),
						rs.getObject("checkout_expires_at", OffsetDateTime.class), rs.getBoolean("manual"),
						rs.getObject("approved_at", OffsetDateTime.class),
						rs.getObject("rejected_at", OffsetDateTime.class),
						rs.getObject("expired_at", OffsetDateTime.class),
						rs.getObject("refunded_at", OffsetDateTime.class),
						rs.getObject("created_at", OffsetDateTime.class), rs.getString("booking_status"),
						rs.getString("booking_payment_status"), rs.getString("subject"), rs.getString("service_name"),
						rs.getString("professional_name"), rs.getString("room_name"),
						rs.getObject("starts_at", OffsetDateTime.class), rs.getInt("duration_minutes"),
						rs.getString("location_name"), rs.getString("customer_name")));
		if (items.isEmpty())
			throw new ResourceNotFoundException("No se encontro el pago de reserva.");
		return items.getFirst();
	}

	public BookingPaymentRecord findById(UUID paymentId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where id = :paymentId",
				new MapSqlParameterSource().addValue("paymentId", paymentId), paymentRowMapper());
		if (items.isEmpty())
			throw new ResourceNotFoundException("No se encontro el pago de reserva.");
		return items.getFirst();
	}

	public BookingPaymentRecord findByIdForBooking(UUID businessId, UUID bookingId, UUID paymentId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where business_id = :businessId "
						+ "and booking_id = :bookingId and id = :paymentId",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
						.addValue("paymentId", paymentId),
				paymentRowMapper());
		if (items.isEmpty())
			throw new ResourceNotFoundException("No se encontro el pago de reserva.");
		return items.getFirst();
	}

	public Optional<BookingPaymentRecord> findByProviderExternalReferenceForUpdate(String provider,
			String externalRef) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_external_reference = :externalRef order by created_at desc limit 1 for update",
				new MapSqlParameterSource().addValue("provider", provider).addValue("externalRef", externalRef),
				paymentRowMapper());
		return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
	}

	public Optional<BookingPaymentRecord> findByProviderPreferenceIdForUpdate(String provider, String preferenceId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_preference_id = :preferenceId order by created_at desc limit 1 for update",
				new MapSqlParameterSource().addValue("provider", provider).addValue("preferenceId", preferenceId),
				paymentRowMapper());
		return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
	}

	public Optional<BookingPaymentRecord> findByProviderPaymentIdForUpdate(String provider, String providerPaymentId) {
		List<BookingPaymentRecord> items = jdbcTemplate.query(
				"select id, business_id, booking_id, provider, provider_payment_id, "
						+ "provider_preference_id, provider_external_reference, "
						+ "idempotency_key, amount, currency, status, " + "payment_purpose, "
						+ "approved_at, rejected_at, expired_at, refunded_at, "
						+ "checkout_url, checkout_expires_at, manual, created_at "
						+ "from booking_payment where provider = :provider "
						+ "and provider_payment_id = :providerPaymentId order by created_at desc limit 1 for update",
				new MapSqlParameterSource().addValue("provider", provider).addValue("providerPaymentId",
						providerPaymentId),
				paymentRowMapper());
		return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
	}

	// ---- Recalculate ----

	public void recalculateBookingPaymentStatus(UUID businessId, UUID bookingId) {
		// First update with approved sums
		jdbcTemplate.update("""
				update booking b
				set payment_status = case
				    when b.requires_deposit = false then 'NOT_REQUIRED'
				    when coalesce(approved.total_approved, 0) >= coalesce(b.deposit_amount, 0) then 'PAID'
				    when coalesce(approved.total_approved, 0) > 0 then 'PARTIAL'
				    when exists (
				        select 1 from booking_payment p
				        where p.business_id = b.business_id and p.booking_id = b.id
				        and p.status in ('REJECTED', 'EXPIRED')
				    ) then 'FAILED'
				    else 'PENDING'
				end,
				updated_at = current_timestamp
				from (
				    select business_id, booking_id, coalesce(sum(amount), 0) as total_approved
				    from booking_payment
				    where business_id = :businessId and booking_id = :bookingId
				    and status = 'APPROVED'
				    group by business_id, booking_id
				) approved
				where b.business_id = :businessId and b.id = :bookingId
				and approved.business_id = b.business_id and approved.booking_id = b.id
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
		// Fallback when no approved payments exist
		jdbcTemplate.update("""
				update booking b
				set payment_status = case
				    when b.requires_deposit = false then 'NOT_REQUIRED'
				    when exists (
				        select 1 from booking_payment p
				        where p.business_id = b.business_id and p.booking_id = b.id
				        and p.status in ('REJECTED', 'EXPIRED')
				    ) then 'FAILED'
				    else 'PENDING'
				end,
				updated_at = current_timestamp
				where b.business_id = :businessId and b.id = :bookingId
				and not exists (
				    select 1 from booking_payment p
				    where p.business_id = b.business_id and p.booking_id = b.id
				    and p.status = 'APPROVED'
				)
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
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
						left join booking_payment p on p.business_id = b.business_id and p.booking_id = b.id
						where b.business_id = :businessId and b.id = :bookingId
						group by b.requires_deposit, b.deposit_amount
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				Boolean.class);
		return Boolean.TRUE.equals(approved);
	}

	// ---- Parameter builders ----

	private MapSqlParameterSource paymentParams(UUID paymentId, UUID businessId, UUID bookingId, String provider,
			String providerPaymentId, String providerPreferenceId, String providerExternalReference,
			String idempotencyKey, BigDecimal amount, String currency, String status, String rawPayload,
			String metadata, OffsetDateTime occurredAt, String statusDetail, String rawStatus, String paymentMethodId,
			Integer installments, String payerEmail) {
		return new MapSqlParameterSource().addValue("id", paymentId).addValue("businessId", businessId)
				.addValue("bookingId", bookingId).addValue("provider", provider)
				.addValue("providerPaymentId", providerPaymentId).addValue("providerPreferenceId", providerPreferenceId)
				.addValue("providerExternalReference", providerExternalReference)
				.addValue("idempotencyKey", idempotencyKey).addValue("amount", amount).addValue("currency", currency)
				.addValue("status", status).addValue("rawPayload", rawPayload).addValue("metadata", metadata)
				.addValue("statusDetail", statusDetail).addValue("rawStatus", rawStatus)
				.addValue("paymentMethodId", paymentMethodId).addValue("installments", installments)
				.addValue("payerEmail", payerEmail)
				.addValue("approvedAt", "APPROVED".equals(status) ? occurredAt : null)
				.addValue("rejectedAt", "REJECTED".equals(status) ? occurredAt : null)
				.addValue("expiredAt", "EXPIRED".equals(status) ? occurredAt : null)
				.addValue("refundedAt", "REFUNDED".equals(status) ? occurredAt : null);
	}

	// ---- Row mappers ----

	private RowMapper<BookingPaymentBookingRecord> bookingRowMapper() {
		return (rs, rowNum) -> new BookingPaymentBookingRecord(rs.getObject("booking_id", UUID.class),
				rs.getObject("business_id", UUID.class), rs.getString("booking_status"),
				rs.getBoolean("requires_deposit"), rs.getBigDecimal("deposit_amount"), rs.getString("payment_status"));
	}

	private RowMapper<BookingPaymentRecord> paymentRowMapper() {
		return (rs, rowNum) -> new BookingPaymentRecord(rs.getObject("id", UUID.class),
				rs.getObject("business_id", UUID.class), rs.getObject("booking_id", UUID.class),
				rs.getString("provider"), rs.getString("provider_payment_id"), rs.getString("provider_preference_id"),
				rs.getString("provider_external_reference"), rs.getString("idempotency_key"),
				rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"),
				rs.getString("payment_purpose"), rs.getObject("approved_at", OffsetDateTime.class),
				rs.getObject("rejected_at", OffsetDateTime.class), rs.getObject("expired_at", OffsetDateTime.class),
				rs.getObject("refunded_at", OffsetDateTime.class), rs.getString("checkout_url"),
				rs.getObject("checkout_expires_at", OffsetDateTime.class), rs.getBoolean("manual"),
				rs.getObject("created_at", OffsetDateTime.class));
	}

	// ---- Records ----

	public record BookingPaymentBookingRecord(UUID bookingId, UUID businessId, String bookingStatus,
			boolean requiresDeposit, BigDecimal depositAmount, String paymentStatus) {
	}

	public record BookingPaymentNotificationRecord(UUID bookingId, UUID businessId, String subject,
			String bookingStatus, OffsetDateTime startsAt, int durationMinutes, String location, String locationName,
			String serviceName, String professionalName, String roomName, String customerName, String customerPhone,
			String customerEmail) {
	}

	public record BookingPaymentRecord(UUID id, UUID businessId, UUID bookingId, String provider,
			String providerPaymentId, String providerPreferenceId, String providerExternalReference,
			String idempotencyKey, BigDecimal amount, String currency, String status, String paymentPurpose,
			OffsetDateTime approvedAt, OffsetDateTime rejectedAt, OffsetDateTime expiredAt, OffsetDateTime refundedAt,
			String checkoutUrl, OffsetDateTime checkoutExpiresAt, boolean manual, OffsetDateTime createdAt) {
	}

	public record PublicBookingPaymentDetailRecord(UUID id, UUID bookingId, String provider, String providerPaymentId,
			BigDecimal amount, String currency, String status, String checkoutUrl, OffsetDateTime checkoutExpiresAt,
			boolean manual, OffsetDateTime approvedAt, OffsetDateTime rejectedAt, OffsetDateTime expiredAt,
			OffsetDateTime refundedAt, OffsetDateTime createdAt, String bookingStatus, String bookingPaymentStatus,
			String subject, String serviceName, String professionalName, String roomName, OffsetDateTime startsAt,
			int durationMinutes, String locationName, String customerName) {
	}
}