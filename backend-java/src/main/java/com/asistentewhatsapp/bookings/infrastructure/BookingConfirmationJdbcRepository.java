package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingConfirmationJdbcRepository {

	private static final List<String> ACTIVE_BOOKING_STATUSES = List.of("REQUESTED", "TEMPORARY",
			"PENDIENTE_CONFIRMACION", "CONFIRMED", "RESCHEDULED", "REPROGRAMADA", "SOLICITADA", "PENDIENTE_PAGO",
			"CONFIRMADA", "REPROGRAMACION_PENDIENTE");
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public BookingConfirmationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public ConfirmationBookingRecord findBooking(UUID businessId, UUID bookingId) {
		List<ConfirmationBookingRecord> items = jdbcTemplate.query(
				"""
						select b.id as booking_id, b.business_id, b.subject, b.status as booking_status,
						       b.starts_at, b.duration_minutes, b.location_id, b.location, b.conversation_id,
						       b.service_id, b.professional_id, b.room_id,
						       b.requires_deposit, coalesce(b.deposit_amount, 0) as deposit_amount, b.payment_status,
						       bl.name as location_name, s.name as service_name, p.full_name as professional_name,
						       r.name as room_name, c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
						from booking b
						join customer c on c.id = b.customer_id and c.business_id = b.business_id
						left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
						left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
						left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
						left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
						where b.business_id = :businessId and b.id = :bookingId
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				bookingRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la cita solicitada.");
		}
		return items.getFirst();
	}

	public ConfirmationLinkRecord findByTokenHash(String tokenHash) {
		return findOneLink("where l.token_hash = :tokenHash",
				new MapSqlParameterSource().addValue("tokenHash", tokenHash));
	}

	public ConfirmationLinkRecord findByTokenHashForUpdate(String tokenHash) {
		return findOneLink("where l.token_hash = :tokenHash for update of l",
				new MapSqlParameterSource().addValue("tokenHash", tokenHash));
	}

	private ConfirmationLinkRecord findOneLink(String whereClause, MapSqlParameterSource params) {
		List<ConfirmationLinkRecord> items = queryLinks(whereClause, params);
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("El enlace de confirmacion no existe o fue invalidado.");
		}
		return items.getFirst();
	}

	public Optional<ConfirmationLinkRecord> findLatestByBooking(UUID businessId, UUID bookingId) {
		return queryLinks(
				"where l.business_id = :businessId and l.booking_id = :bookingId order by l.created_at desc limit 1",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId))
				.stream().findFirst();
	}

	public Optional<ConfirmationLinkRecord> findLatestActionableByConversation(UUID businessId, UUID conversationId) {
		return queryLinks("where l.business_id = :businessId and b.conversation_id = :conversationId "
				+ "and b.status in ('PENDIENTE_CONFIRMACION', 'CONFIRMED', 'CONFIRMADA', 'RESCHEDULED', 'REPROGRAMADA') "
				+ "and l.status not in ('EXPIRED') order by b.created_at desc limit 1",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
						conversationId))
				.stream().findFirst();
	}

	private List<ConfirmationLinkRecord> queryLinks(String whereClause, MapSqlParameterSource params) {
		return jdbcTemplate.query("""
				select l.id as link_id, l.business_id, l.booking_id, l.status as link_status,
				       l.confirmation_url, l.expires_at, l.sent_at, l.opened_at, l.confirmed_at,
				       b.subject, b.status as booking_status, b.starts_at, b.duration_minutes,
				       b.location_id, b.location, b.service_id, b.professional_id, b.room_id, bl.name as location_name,
				       b.requires_deposit, coalesce(b.deposit_amount, 0) as deposit_amount, b.payment_status,
				       b.conversation_id,
				       s.name as service_name, p.full_name as professional_name, r.name as room_name,
				       c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
				from booking_confirmation_link l
				join booking b on b.id = l.booking_id and b.business_id = l.business_id
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				""" + whereClause, params, linkRowMapper());
	}

	public UUID insertLink(UUID businessId, UUID bookingId, String tokenHash, String confirmationUrl,
			OffsetDateTime expiresAt) {
		UUID linkId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into booking_confirmation_link (id, business_id, booking_id, token_hash, confirmation_url, status, expires_at, sent_at)
						values (:id, :businessId, :bookingId, :tokenHash, :confirmationUrl, 'GENERATED', :expiresAt, null)
						""",
				new MapSqlParameterSource().addValue("id", linkId).addValue("businessId", businessId)
						.addValue("bookingId", bookingId).addValue("tokenHash", tokenHash)
						.addValue("confirmationUrl", confirmationUrl).addValue("expiresAt", expiresAt));
		return linkId;
	}

	public void markSent(UUID linkId, OffsetDateTime sentAt) {
		jdbcTemplate.update("""
				update booking_confirmation_link
				set status = 'SENT', sent_at = coalesce(sent_at, :sentAt), updated_at = current_timestamp
				where id = :linkId and status = 'GENERATED'
				""", new MapSqlParameterSource().addValue("linkId", linkId).addValue("sentAt", sentAt));
	}

	public void invalidateActiveLinks(UUID businessId, UUID bookingId) {
		jdbcTemplate.update(
				"""
						update booking_confirmation_link
						set status = 'INVALIDATED', invalidated_at = current_timestamp, updated_at = current_timestamp
						where business_id = :businessId and booking_id = :bookingId and status in ('GENERATED', 'SENT', 'OPENED')
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
	}

	public void markOpened(UUID linkId) {
		jdbcTemplate.update("""
				update booking_confirmation_link
				set status = case when status = 'GENERATED' then 'OPENED' else status end,
				    opened_at = coalesce(opened_at, current_timestamp), updated_at = current_timestamp
				where id = :linkId and status in ('GENERATED', 'SENT', 'OPENED')
				""", new MapSqlParameterSource().addValue("linkId", linkId));
	}

	public void markConfirmed(UUID linkId) {
		jdbcTemplate.update(
				"""
						update booking_confirmation_link
						set status = 'CONFIRMED', confirmed_at = coalesce(confirmed_at, current_timestamp), updated_at = current_timestamp
						where id = :linkId and status in ('GENERATED', 'SENT', 'OPENED')
						""",
				new MapSqlParameterSource().addValue("linkId", linkId));
	}

	public void markExpired(UUID linkId) {
		jdbcTemplate.update(
				"""
						update booking_confirmation_link
						set status = 'EXPIRED', expired_at = coalesce(expired_at, current_timestamp), updated_at = current_timestamp
						where id = :linkId and status in ('GENERATED', 'SENT', 'OPENED')
						""",
				new MapSqlParameterSource().addValue("linkId", linkId));
	}

	public void updateBookingStatus(UUID businessId, UUID bookingId, String status) {
		int updated = jdbcTemplate.update("""
				update booking
				set status = :status,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = :businessId and id = :bookingId
				  and status in (:activeStatuses)
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
				.addValue("status", status).addValue("activeStatuses", ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro una reserva activa para confirmar.");
		}
	}

	public int expireDueLinks(OffsetDateTime now) {
		return jdbcTemplate.update(
				"""
						update booking_confirmation_link
						set status = 'EXPIRED', expired_at = coalesce(expired_at, current_timestamp), updated_at = current_timestamp
						where status in ('GENERATED', 'SENT', 'OPENED') and expires_at <= :now
						""",
				new MapSqlParameterSource().addValue("now", now));
	}

	public int expireBookingsWithExpiredLinks(OffsetDateTime now) {
		return jdbcTemplate.update(
				"""
						update booking b
						set status = 'EXPIRADA',
						    version = version + 1,
						    updated_at = current_timestamp
						where b.status in ('PENDIENTE_CONFIRMACION', 'TEMPORARY', 'REQUESTED', 'SOLICITADA', 'PENDIENTE_PAGO')
						  and exists (
						      select 1 from booking_confirmation_link l
						      where l.business_id = b.business_id and l.booking_id = b.id and l.status = 'EXPIRED' and l.expires_at <= :now
						  )
						  and not exists (
						      select 1 from booking_confirmation_link active_link
						      where active_link.business_id = b.business_id
						        and active_link.booking_id = b.id
						        and active_link.status in ('GENERATED', 'SENT', 'OPENED')
						  )
						""",
				new MapSqlParameterSource().addValue("now", now));
	}

	public boolean hasOverlappingActiveBooking(UUID businessId, UUID bookingId, UUID locationId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		if (locationId == null) {
			return false;
		}
		StringBuilder sql = new StringBuilder("""
				select exists (
				    select 1 from booking b
				    where b.business_id = :businessId
				      and b.location_id = :locationId
				      and b.status in (:activeStatuses)
				      and b.starts_at < :endsAt
				      and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		if (bookingId != null) {
			sql.append(" and b.id <> :bookingId\n");
			params.addValue("bookingId", bookingId);
		}
		sql.append(")\n");
		Boolean exists = jdbcTemplate.queryForObject(sql.toString(), params, Boolean.class);
		return Boolean.TRUE.equals(exists);
	}

	private RowMapper<ConfirmationBookingRecord> bookingRowMapper() {
		return (resultSet, rowNum) -> new ConfirmationBookingRecord(resultSet.getObject("booking_id", UUID.class),
				resultSet.getObject("business_id", UUID.class), resultSet.getString("subject"),
				resultSet.getString("booking_status"), resultSet.getObject("starts_at", OffsetDateTime.class),
				resultSet.getInt("duration_minutes"), resultSet.getObject("location_id", UUID.class),
				resultSet.getString("location"), resultSet.getObject("conversation_id", UUID.class),
				resultSet.getObject("service_id", UUID.class), resultSet.getObject("professional_id", UUID.class),
				resultSet.getObject("room_id", UUID.class), resultSet.getString("location_name"),
				resultSet.getString("service_name"), resultSet.getString("professional_name"),
				resultSet.getString("room_name"), resultSet.getString("customer_name"),
				resultSet.getString("customer_phone"), resultSet.getString("customer_email"),
				resultSet.getBoolean("requires_deposit"), resultSet.getBigDecimal("deposit_amount"),
				resultSet.getString("payment_status"));
	}

	private RowMapper<ConfirmationLinkRecord> linkRowMapper() {
		return (resultSet, rowNum) -> new ConfirmationLinkRecord(resultSet.getObject("link_id", UUID.class),
				resultSet.getObject("business_id", UUID.class), resultSet.getObject("booking_id", UUID.class),
				resultSet.getString("link_status"), resultSet.getString("confirmation_url"),
				resultSet.getObject("expires_at", OffsetDateTime.class),
				resultSet.getObject("sent_at", OffsetDateTime.class),
				resultSet.getObject("opened_at", OffsetDateTime.class),
				resultSet.getObject("confirmed_at", OffsetDateTime.class), resultSet.getString("subject"),
				resultSet.getString("booking_status"), resultSet.getString("service_name"),
				resultSet.getString("professional_name"), resultSet.getString("room_name"),
				resultSet.getObject("starts_at", OffsetDateTime.class), resultSet.getInt("duration_minutes"),
				resultSet.getObject("location_id", UUID.class), resultSet.getString("location"),
				resultSet.getObject("service_id", UUID.class), resultSet.getObject("professional_id", UUID.class),
				resultSet.getObject("room_id", UUID.class), resultSet.getString("location_name"),
				resultSet.getString("customer_name"), resultSet.getString("customer_phone"),
				resultSet.getString("customer_email"), resultSet.getBoolean("requires_deposit"),
				resultSet.getBigDecimal("deposit_amount"), resultSet.getString("payment_status"),
				resultSet.getObject("conversation_id", UUID.class));
	}

	public record ConfirmationBookingRecord(UUID bookingId, UUID businessId, String subject, String bookingStatus,
			OffsetDateTime startsAt, int durationMinutes, UUID locationId, String location, UUID conversationId,
			UUID serviceId, UUID professionalId, UUID roomId, String locationName, String serviceName,
			String professionalName, String roomName, String customerName, String customerPhone, String customerEmail,
			boolean requiresDeposit, BigDecimal depositAmount, String paymentStatus) {
	}

	public record ConfirmationLinkRecord(UUID linkId, UUID businessId, UUID bookingId, String linkStatus,
			String confirmationUrl, OffsetDateTime expiresAt, OffsetDateTime sentAt, OffsetDateTime openedAt,
			OffsetDateTime confirmedAt, String subject, String bookingStatus, String serviceName,
			String professionalName, String roomName, OffsetDateTime startsAt, int durationMinutes, UUID locationId,
			String location, UUID serviceId, UUID professionalId, UUID roomId, String locationName, String customerName,
			String customerPhone, String customerEmail, boolean requiresDeposit, BigDecimal depositAmount,
			String paymentStatus, UUID conversationId) {
	}
}
