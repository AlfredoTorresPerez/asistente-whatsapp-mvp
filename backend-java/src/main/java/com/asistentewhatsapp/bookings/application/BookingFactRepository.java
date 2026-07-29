package com.asistentewhatsapp.bookings.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingFactRepository {

	private final JdbcTemplate jdbcTemplate;

	public BookingFactRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<BookingFact> findActiveByPhone(UUID businessId, String phone) {
		return jdbcTemplate.query("""
				select booking_id, business_id, customer_phone, customer_name,
				       customer_management_id, service_name, location_name, professional_name,
				       booking_date, booking_time, booking_status, conversation_id,
				       channel_origin, origin_intent, tiene_reserva_activa,
				       booking_created_at, sync_status, synced_at
				from ia_hecho_reserva
				where business_id = ?
				  and (customer_phone = ? or customer_management_id = ?)
				  and tiene_reserva_activa = true
				  and sync_status = 'SYNCED'
				order by booking_date desc nulls last, booking_created_at desc
				limit 20
				""", (rs, rowNum) -> new BookingFact(rs.getObject("booking_id", UUID.class),
				rs.getObject("business_id", UUID.class), rs.getString("customer_phone"), rs.getString("customer_name"),
				rs.getString("customer_management_id"), rs.getString("service_name"), rs.getString("location_name"),
				rs.getString("professional_name"),
				Optional.ofNullable(rs.getObject("booking_date", java.sql.Date.class)).map(java.sql.Date::toLocalDate)
						.orElse(null),
				Optional.ofNullable(rs.getObject("booking_time", java.sql.Time.class)).map(java.sql.Time::toLocalTime)
						.orElse(null),
				rs.getString("booking_status"),
				Optional.ofNullable(rs.getObject("conversation_id", UUID.class)).orElse(null),
				rs.getString("channel_origin"), rs.getString("origin_intent"), rs.getBoolean("tiene_reserva_activa"),
				rs.getObject("booking_created_at", OffsetDateTime.class)), businessId, phone, phone);
	}

	public boolean hasActiveBooking(UUID businessId, String phone) {
		Boolean exists = jdbcTemplate.queryForObject("""
				select exists (
				    select 1 from ia_hecho_reserva
				    where business_id = ?
				      and (customer_phone = ? or customer_management_id = ?)
				      and tiene_reserva_activa = true
				      and sync_status = 'SYNCED'
				)
				""", Boolean.class, businessId, phone, phone);
		return Boolean.TRUE.equals(exists);
	}

	public void markInactive(UUID bookingId) {
		jdbcTemplate.update("""
				update ia_hecho_reserva
				set tiene_reserva_activa = false,
				    updated_at = current_timestamp
				where booking_id = ?
				""", bookingId);
	}

	public record BookingFact(UUID bookingId, UUID businessId, String customerPhone, String customerName,
			String customerManagementId, String serviceName, String locationName, String professionalName,
			LocalDate bookingDate, LocalTime bookingTime, String bookingStatus, UUID conversationId,
			String channelOrigin, String originIntent, boolean tieneReservaActiva, OffsetDateTime bookingCreatedAt) {
	}
}
