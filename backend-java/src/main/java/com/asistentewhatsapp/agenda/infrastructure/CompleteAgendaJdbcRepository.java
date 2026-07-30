package com.asistentewhatsapp.agenda.infrastructure;

import com.asistentewhatsapp.agenda.api.AgendaBlockResponse;
import com.asistentewhatsapp.agenda.api.AgendaCalendarItemResponse;
import com.asistentewhatsapp.agenda.api.AgendaFilterOptionResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import com.asistentewhatsapp.shared.observability.CorrelationIdFilter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Repository
public class CompleteAgendaJdbcRepository {

	private static final Logger log = LoggerFactory.getLogger(CompleteAgendaJdbcRepository.class);
	private static final List<String> ACTIVE_BOOKING_STATUSES = List.of("REQUESTED", "TEMPORARY",
			"PENDIENTE_CONFIRMACION", "CONFIRMED", "RESCHEDULED", "REPROGRAMADA", "SOLICITADA", "PENDIENTE_PAGO",
			"CONFIRMADA", "REPROGRAMACION_PENDIENTE");

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public CompleteAgendaJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private void logInput(String method, Object... args) {
		log.info("{} - IN - {}", method, describeArgs(args));
	}

	private void logOutput(String method, Object result) {
		log.info("{} - OUT - {}", method, describe(result));
	}

	private String describeArgs(Object... args) {
		return Arrays.stream(args).map(this::describe).collect(Collectors.joining(", ", "[", "]"));
	}

	private String describe(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Optional<?> optional) {
			return optional.map(this::describe).orElse("Optional.empty");
		}
		if (value instanceof Collection<?> collection) {
			return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
		}
		if (value.getClass().isArray()) {
			return value.getClass().getComponentType().getSimpleName() + "[]";
		}
		return String.valueOf(value);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	public LocationRecord findLocation(UUID businessId, UUID locationId) {
		logInput("findLocation", businessId, locationId);
		List<LocationRecord> items = jdbcTemplate.query("""
				select id, name, timezone
				from business_location
				where business_id = :businessId
				  and id = :locationId
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
				(rs, rowNum) -> new LocationRecord(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getString("timezone")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la sucursal activa indicada.");
		}
		LocationRecord result = items.getFirst();
		logOutput("findLocation", result);
		return result;
	}

	public ServiceRecord findService(UUID businessId, UUID locationId, UUID serviceId) {
		logInput("findService", businessId, locationId, serviceId);
		List<ServiceRecord> items = jdbcTemplate.query("""
				select
				    s.id,
				    s.name,
				    coalesce(sl.duration_override_minutes, s.duration_minutes) as duration_minutes,
				    s.requires_room,
				    s.requires_deposit,
				    coalesce(s.deposit_amount, 0) as deposit_amount,
				    s.preparation_minutes,
				    s.cleanup_minutes,
				    s.active,
				    s.price_base,
				    s.requires_prior_evaluation,
				    s.requires_informed_consent
				from aesthetic_service s
				left join aesthetic_service_location sl
				  on sl.business_id = s.business_id
				 and sl.service_id = s.id
				 and sl.location_id = :locationId
				 and sl.active = true
				where s.business_id = :businessId
				  and s.id = :serviceId
				  and s.active = true
				  and (sl.id is not null or not exists (
				      select 1 from aesthetic_service_location x
				      where x.business_id = s.business_id
				        and x.service_id = s.id
				        and x.active = true
				  ))
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
						.addValue("serviceId", serviceId),
				(rs, rowNum) -> new ServiceRecord(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getInt("duration_minutes"), rs.getBoolean("requires_room"),
						rs.getBoolean("requires_deposit"), rs.getBigDecimal("deposit_amount"),
						rs.getInt("preparation_minutes"), rs.getInt("cleanup_minutes"), rs.getBoolean("active"),
						rs.getBigDecimal("price_base"), rs.getBoolean("requires_prior_evaluation"),
						rs.getBoolean("requires_informed_consent")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("El servicio no esta disponible en la sucursal seleccionada.");
		}
		ServiceRecord result = items.getFirst();
		logOutput("findService", result);
		return result;
	}

	public List<AgendaFilterOptionResponse> findServiceFilterOptions(UUID businessId, UUID locationId) {
		logInput("findServiceFilterOptions", businessId, locationId);
		StringBuilder sql = new StringBuilder("""
				select distinct
				    s.id,
				    s.name,
				    cast(coalesce(sl.duration_override_minutes, s.duration_minutes) as text) || ' min' as detail,
				    sl.location_id,
				    s.active
				from aesthetic_service s
				left join aesthetic_service_location sl
				  on sl.business_id = s.business_id
				 and sl.service_id = s.id
				 and sl.active = true
				where s.business_id = :businessId
				  and s.active = true
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId);
		if (locationId != null) {
			sql.append(" and (sl.location_id = :locationId or not exists (\n")
					.append("     select 1 from aesthetic_service_location x\n")
					.append("     where x.business_id = s.business_id\n").append("       and x.service_id = s.id\n")
					.append("       and x.active = true\n").append(" ))\n");
			params.addValue("locationId", locationId);
		}
		sql.append(" order by s.name\n");
		List<AgendaFilterOptionResponse> result = jdbcTemplate.query(sql.toString(), params, filterOptionMapper());
		logOutput("findServiceFilterOptions", result);
		return result;
	}

	public List<AgendaFilterOptionResponse> findProfessionalFilterOptions(UUID businessId, UUID locationId) {
		logInput("findProfessionalFilterOptions", businessId, locationId);
		StringBuilder sql = new StringBuilder("""
				select distinct
				    p.id,
				    p.full_name as name,
				    p.specialty as detail,
				    pl.location_id,
				    p.active
				from aesthetic_professional p
				left join aesthetic_professional_location pl
				  on pl.business_id = p.business_id
				 and pl.professional_id = p.id
				 and pl.active = true
				where p.business_id = :businessId
				  and p.active = true
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId);
		if (locationId != null) {
			sql.append(" and pl.location_id = :locationId\n");
			params.addValue("locationId", locationId);
		}
		sql.append(" order by p.full_name\n");
		List<AgendaFilterOptionResponse> result = jdbcTemplate.query(sql.toString(), params, filterOptionMapper());
		logOutput("findProfessionalFilterOptions", result);
		return result;
	}

	public List<AgendaFilterOptionResponse> findRoomFilterOptions(UUID businessId, UUID locationId) {
		logInput("findRoomFilterOptions", businessId, locationId);
		StringBuilder sql = new StringBuilder("""
				select distinct
				    r.id,
				    r.name,
				    r.room_type as detail,
				    r.location_id,
				    r.active
				from agenda_room r
				where r.business_id = :businessId
				  and r.active = true
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId);
		if (locationId != null) {
			sql.append(" and r.location_id = :locationId\n");
			params.addValue("locationId", locationId);
		}
		sql.append(" order by r.name\n");
		List<AgendaFilterOptionResponse> result = jdbcTemplate.query(sql.toString(), params, filterOptionMapper());
		logOutput("findRoomFilterOptions", result);
		return result;
	}

	public List<TimeWindowRecord> findBusinessHours(UUID businessId, UUID locationId, int dayOfWeek) {
		logInput("findBusinessHours", businessId, locationId, dayOfWeek);
		List<TimeWindowRecord> result = jdbcTemplate.query("""
				select start_time, end_time
				from agenda_business_hours
				where business_id = :businessId
				  and location_id = :locationId
				  and day_of_week = :dayOfWeek
				  and active = true
				order by start_time
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("dayOfWeek", dayOfWeek), timeWindowMapper());
		logOutput("findBusinessHours", result);
		return result;
	}

	public List<TimeWindowRecord> findProfessionalHours(UUID businessId, UUID locationId, UUID professionalId,
			int dayOfWeek) {
		logInput("findProfessionalHours", businessId, locationId, professionalId, dayOfWeek);
		List<TimeWindowRecord> result = jdbcTemplate.query("""
				select start_time, end_time
				from agenda_professional_hours
				where business_id = :businessId
				  and location_id = :locationId
				  and professional_id = :professionalId
				  and day_of_week = :dayOfWeek
				  and active = true
				order by start_time
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
						.addValue("professionalId", professionalId).addValue("dayOfWeek", dayOfWeek),
				timeWindowMapper());
		logOutput("findProfessionalHours", result);
		return result;
	}

	public boolean isHoliday(UUID businessId, UUID locationId, LocalDate date) {
		logInput("isHoliday", businessId, locationId, date);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from agenda_holiday
				where business_id = :businessId
				  and holiday_date = :date
				  and active = true
				  and (location_id is null or location_id = :locationId)
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("date", date), Integer.class);
		boolean result = count != null && count > 0;
		logOutput("isHoliday", result);
		return result;
	}

	public boolean isCustomerBlocked(UUID businessId, String phone) {
		logInput("isCustomerBlocked", businessId, phone);
		if (phone == null || phone.isBlank()) {
			logOutput("isCustomerBlocked", false);
			return false;
		}
		String normalized = phone.replaceAll("\\D", "");
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from customer
				where business_id = :businessId
				  and active = false
				  and (normalized_phone = :phone or regexp_replace(coalesce(phone, ''), '\\D', '', 'g') = :phone)
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("phone", normalized),
				Integer.class);
		boolean result = count != null && count > 0;
		logOutput("isCustomerBlocked", result);
		return result;
	}

	public boolean hasExcessiveNoShows(UUID businessId, String phone) {
		logInput("hasExcessiveNoShows", businessId, phone);
		if (phone == null || phone.isBlank()) {
			logOutput("hasExcessiveNoShows", false);
			return false;
		}
		String normalized = phone.replaceAll("\\D", "");
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				where b.business_id = :businessId
				  and b.status = 'NO_ASISTE'
				  and b.starts_at >= current_timestamp - interval '6 months'
				  and (c.normalized_phone = :phone or regexp_replace(coalesce(c.phone, ''), '\\D', '', 'g') = :phone)
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("phone", normalized),
				Integer.class);
		boolean result = count != null && count >= 3;
		logOutput("hasExcessiveNoShows", result);
		return result;
	}

	public boolean isServiceCategoryActive(UUID businessId, UUID serviceId) {
		logInput("isServiceCategoryActive", businessId, serviceId);
		String categoryActive = jdbcTemplate.queryForObject("""
				select c.active::text
				from aesthetic_service s
				join aesthetic_service_category c on c.id = s.category_id and c.business_id = s.business_id
				where s.business_id = :businessId and s.id = :serviceId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				String.class);
		boolean result = "true".equals(categoryActive);
		logOutput("isServiceCategoryActive", result);
		return result;
	}

	public boolean isLocationServingService(UUID businessId, UUID locationId, UUID serviceId) {
		logInput("isLocationServingService", businessId, locationId, serviceId);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*) from (
				    select sl.service_id
				    from aesthetic_service_location sl
				    where sl.business_id = :businessId
				      and sl.location_id = :locationId
				      and sl.service_id = :serviceId
				      and sl.active = true
				    union all
				    select s.id
				    from aesthetic_service s
				    where s.business_id = :businessId
				      and s.id = :serviceId
				      and s.active = true
				      and not exists (
				          select 1 from aesthetic_service_location x
				          where x.business_id = s.business_id
				            and x.service_id = s.id
				            and x.active = true
				      )
				) combined
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("serviceId", serviceId), Integer.class);
		boolean result = count != null && count > 0;
		logOutput("isLocationServingService", result);
		return result;
	}

	public boolean isLocationClosedForMaintenance(UUID businessId, UUID locationId, OffsetDateTime startsAt) {
		logInput("isLocationClosedForMaintenance", businessId, locationId, startsAt);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from agenda_block b
				where b.business_id = :businessId
				  and b.active = true
				  and b.location_id = :locationId
				  and b.professional_id is null
				  and b.room_id is null
				  and b.starts_at <= :startsAt
				  and b.ends_at > :startsAt
				  and b.reason = 'MAINTENANCE'
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("startsAt", startsAt), Integer.class);
		boolean result = count != null && count > 0;
		logOutput("isLocationClosedForMaintenance", result);
		return result;
	}

	public boolean isProfessionalActive(UUID businessId, UUID professionalId) {
		logInput("isProfessionalActive", businessId, professionalId);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from aesthetic_professional
				where business_id = :businessId
				  and id = :professionalId
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), Integer.class);
		boolean result = count != null && count > 0;
		logOutput("isProfessionalActive", result);
		return result;
	}

	public UUID findBookingByNotesIdempotency(UUID businessId, String idempotencyKey) {
		logInput("findBookingByNotesIdempotency", businessId, idempotencyKey);
		String searchPattern = "[IDEM:" + idempotencyKey + "]";
		List<UUID> items = jdbcTemplate.query("""
				select id from booking
				where business_id = :businessId
				  and notes like :pattern
				  and status in (:activeStatuses)
				order by created_at desc limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("pattern", "%" + searchPattern + "%").addValue("activeStatuses", ACTIVE_BOOKING_STATUSES),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		UUID result = items.isEmpty() ? null : items.getFirst();
		logOutput("findBookingByNotesIdempotency", result);
		return result;
	}

	public void storeIdempotencyInNotes(UUID businessId, UUID bookingId, String idempotencyKey) {
		logInput("storeIdempotencyInNotes", businessId, bookingId, idempotencyKey);
		String tag = "[IDEM:" + idempotencyKey + "]";
		jdbcTemplate.update("""
				update booking
				set notes = case
				    when notes is null or notes = '' then :tag
				    else :tag || ' ' || notes
				end,
				updated_at = current_timestamp
				where business_id = :businessId and id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
				.addValue("tag", tag));
		logOutput("storeIdempotencyInNotes", "done");
	}

	public BookingOperationIdempotencyRecord findBookingOperationIdempotency(UUID businessId, String operationType,
			String idempotencyKey) {
		if (businessId == null || operationType == null || idempotencyKey == null || idempotencyKey.isBlank()) {
			return null;
		}
		List<BookingOperationIdempotencyRecord> records = jdbcTemplate.query("""
				select operation_type, idempotency_key, request_hash, status, booking_id
				from agenda_booking_operation_idempotency
				where business_id = :businessId
				  and operation_type = :operationType
				  and idempotency_key = :idempotencyKey
				limit 1
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("operationType", operationType)
						.addValue("idempotencyKey", idempotencyKey.trim()),
				(rs, rowNum) -> new BookingOperationIdempotencyRecord(rs.getString("operation_type"),
						rs.getString("idempotency_key"), rs.getString("request_hash"), rs.getString("status"),
						rs.getObject("booking_id", UUID.class)));
		return records.isEmpty() ? null : records.getFirst();
	}

	public boolean reserveBookingOperationIdempotency(UUID businessId, String operationType, String idempotencyKey,
			String requestHash, String source) {
		if (businessId == null || operationType == null || idempotencyKey == null || idempotencyKey.isBlank()
				|| requestHash == null || source == null || source.isBlank()) {
			return false;
		}
		int inserted = jdbcTemplate.update("""
				insert into agenda_booking_operation_idempotency (
				    id, business_id, operation_type, idempotency_key, request_hash, source, status
				)
				values (
				    :id, :businessId, :operationType, :idempotencyKey, :requestHash, :source, 'IN_PROGRESS'
				)
				on conflict (business_id, operation_type, idempotency_key) do nothing
				""",
				new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
						.addValue("operationType", operationType).addValue("idempotencyKey", idempotencyKey.trim())
						.addValue("requestHash", requestHash).addValue("source", source.trim()));
		return inserted > 0;
	}

	public void completeBookingOperationIdempotency(UUID businessId, String operationType, String idempotencyKey,
			String requestHash, UUID bookingId) {
		if (businessId == null || operationType == null || idempotencyKey == null || idempotencyKey.isBlank()
				|| requestHash == null || bookingId == null) {
			return;
		}
		jdbcTemplate.update("""
				update agenda_booking_operation_idempotency
				set status = 'COMPLETED',
				    booking_id = :bookingId,
				    request_hash = :requestHash,
				    result = jsonb_build_object('bookingId', cast(:bookingId as text), 'status', 'COMPLETED'),
				    completed_at = current_timestamp,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and operation_type = :operationType
				  and idempotency_key = :idempotencyKey
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("operationType", operationType)
						.addValue("idempotencyKey", idempotencyKey.trim()).addValue("requestHash", requestHash)
						.addValue("bookingId", bookingId));
	}

	public void recordBookingConsent(UUID businessId, UUID bookingId, boolean requiresInformedConsent,
			boolean informedConsentAccepted, LocalDate customerBirthDate, String guardianName, String guardianPhone) {
		if (businessId == null || bookingId == null) {
			return;
		}
		jdbcTemplate.update("""
				update booking
				set requires_informed_consent = :requiresInformedConsent,
				    informed_consent_accepted = :informedConsentAccepted,
				    informed_consent_accepted_at = case
				        when :informedConsentAccepted then coalesce(informed_consent_accepted_at, current_timestamp)
				        else null
				    end,
				    customer_birth_date = :customerBirthDate,
				    guardian_name = :guardianName,
				    guardian_phone = :guardianPhone,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
				.addValue("requiresInformedConsent", requiresInformedConsent)
				.addValue("informedConsentAccepted", informedConsentAccepted)
				.addValue("customerBirthDate", customerBirthDate).addValue("guardianName", trimToNull(guardianName))
				.addValue("guardianPhone", trimToNull(guardianPhone)));
	}

	public List<ProfessionalRecord> findProfessionalCandidates(UUID businessId, UUID locationId, UUID serviceId,
			UUID professionalId) {
		logInput("findProfessionalCandidates", businessId, locationId, serviceId, professionalId);
		StringBuilder sql = new StringBuilder("""
				select p.id, p.full_name
				from aesthetic_professional p
				join aesthetic_service s
				  on s.business_id = p.business_id
				 and s.id = :serviceId
				join aesthetic_professional_location pl
				  on pl.business_id = p.business_id
				 and pl.professional_id = p.id
				 and pl.location_id = :locationId
				 and pl.active = true
				left join agenda_professional_service ps
				  on ps.business_id = p.business_id
				 and ps.professional_id = p.id
				 and ps.service_id = :serviceId
				 and ps.active = true
				where p.business_id = :businessId
				  and p.active = true
				  and coalesce(p.qualification_level, 0) >= coalesce(s.required_professional_level, 0)
				  and (
				      s.requires_professional_certification = false
				      or (p.certification_valid_until is not null and p.certification_valid_until >= current_date)
				  )
				  and (ps.id is not null or not exists (
				      select 1 from agenda_professional_service x
				      where x.business_id = p.business_id
				        and x.professional_id = p.id
				        and x.active = true
				  ))
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("serviceId", serviceId)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		if (professionalId != null) {
			sql.append(" and p.id = :professionalId\n");
			params.addValue("professionalId", professionalId);
		}
		sql.append("""
				 order by (
				     select count(*)
				     from booking b
				     where b.business_id = p.business_id
				       and b.professional_id = p.id
				       and b.status in (:activeStatuses)
				       and b.starts_at::date = current_date
				 ) asc, p.full_name
				""");
		List<ProfessionalRecord> result = jdbcTemplate.query(sql.toString(), params,
				(rs, rowNum) -> new ProfessionalRecord(rs.getObject("id", UUID.class), rs.getString("full_name")));
		logOutput("findProfessionalCandidates", result);
		return result;
	}

	public List<RoomRecord> findRoomCandidates(UUID businessId, UUID locationId, UUID serviceId, UUID roomId) {
		logInput("findRoomCandidates", businessId, locationId, serviceId, roomId);
		StringBuilder sql = new StringBuilder("""
				select distinct r.id, r.name, r.capacity
				from agenda_room r
				left join agenda_room_service rs
				  on rs.business_id = r.business_id
				 and rs.room_id = r.id
				 and rs.service_id = :serviceId
				 and rs.active = true
				where r.business_id = :businessId
				  and r.location_id = :locationId
				  and r.active = true
				  and (rs.id is not null or not exists (
				      select 1 from agenda_room_service x
				      where x.business_id = r.business_id
				        and x.room_id = r.id
				        and x.active = true
				  ))
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("serviceId", serviceId);
		if (roomId != null) {
			sql.append(" and r.id = :roomId\n");
			params.addValue("roomId", roomId);
		}
		sql.append(" order by r.name\n");
		List<RoomRecord> result = jdbcTemplate.query(sql.toString(), params, (rs,
				rowNum) -> new RoomRecord(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("capacity")));
		logOutput("findRoomCandidates", result);
		return result;
	}

	public boolean hasConflict(UUID businessId, UUID bookingId, UUID locationId, UUID professionalId, UUID roomId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		logInput("hasConflict", businessId, bookingId, locationId, professionalId, roomId, startsAt, endsAt);
		if (hasLocationDailyCapacityConflict(businessId, bookingId, locationId, startsAt)) {
			logOutput("hasConflict", true);
			return true;
		}
		if (professionalId != null
				&& hasProfessionalConflict(businessId, bookingId, professionalId, startsAt, endsAt)) {
			logOutput("hasConflict", true);
			return true;
		}
		if (professionalId != null
				&& hasProfessionalTravelConflict(businessId, bookingId, professionalId, locationId, startsAt, endsAt)) {
			logOutput("hasConflict", true);
			return true;
		}
		if (roomId != null) {
			boolean result = hasRoomCapacityConflict(businessId, bookingId, locationId, roomId, startsAt, endsAt);
			logOutput("hasConflict", result);
			return result;
		}
		if (professionalId != null) {
			logOutput("hasConflict", false);
			return false;
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				  and b.location_id = :locationId
				  and b.starts_at < :endsAt
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				""");
		if (bookingId != null) {
			sql.append(" and b.id <> :bookingId\n");
			params.addValue("bookingId", bookingId);
		}
		if (roomId != null) {
			sql.append(" and b.room_id = :roomId\n");
			params.addValue("roomId", roomId);
		}
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		boolean result = count != null && count > 0;
		logOutput("hasConflict", result);
		return result;
	}

	public boolean hasLocationDailyCapacityConflict(UUID businessId, UUID bookingId, UUID locationId,
			OffsetDateTime startsAt) {
		if (businessId == null || locationId == null || startsAt == null) {
			return false;
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("bookingId", bookingId).addValue("locationId", locationId).addValue("startsAt", startsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		List<Boolean> result = jdbcTemplate.query("""
				select case
				    when bl.daily_booking_capacity is null then false
				    else count(b.id) >= bl.daily_booking_capacity
				end as full
				from business_location bl
				left join booking b
				  on b.business_id = bl.business_id
				 and b.location_id = bl.id
				 and b.status in (:activeStatuses)
				 and (cast(:bookingId as uuid) is null or b.id <> :bookingId)
				 and (b.starts_at at time zone coalesce(nullif(bl.timezone, ''), 'UTC'))::date =
				     (cast(:startsAt as timestamptz) at time zone coalesce(nullif(bl.timezone, ''), 'UTC'))::date
				where bl.business_id = :businessId
				  and bl.id = :locationId
				  and bl.active = true
				group by bl.daily_booking_capacity
				""", params, (rs, rowNum) -> rs.getBoolean("full"));
		return result != null && !result.isEmpty() && result.getFirst();
	}

	private void lockAndAssertLocationDailyCapacity(UUID businessId, UUID bookingId, UUID locationId,
			OffsetDateTime startsAt) {
		List<UUID> locked = jdbcTemplate.query("""
				select id
				from business_location
				where business_id = :businessId
				  and id = :locationId
				  and active = true
				for update
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		if (locked.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la sede indicada.");
		}
		if (hasLocationDailyCapacityConflict(businessId, bookingId, locationId, startsAt)) {
			throw new DataIntegrityViolationException("LOCATION_DAILY_CAPACITY_EXCEEDED");
		}
	}

	private boolean hasRoomCapacityConflict(UUID businessId, UUID bookingId, UUID locationId, UUID roomId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("bookingId", bookingId).addValue("locationId", locationId).addValue("roomId", roomId)
				.addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		List<Boolean> result = jdbcTemplate.query("""
				select count(b.id) >= r.capacity as full
				from agenda_room r
				left join booking b
				  on b.business_id = r.business_id
				 and b.room_id = r.id
				 and b.status in (:activeStatuses)
				 and b.starts_at < :endsAt
				 and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				 and (cast(:bookingId as uuid) is null or b.id <> :bookingId)
				where r.business_id = :businessId
				  and r.location_id = :locationId
				  and r.id = :roomId
				  and r.active = true
				group by r.capacity
				""", params, (rs, rowNum) -> rs.getBoolean("full"));
		return result.isEmpty() || result.getFirst();
	}

	private void lockAndAssertRoomCapacity(UUID businessId, UUID bookingId, UUID locationId, UUID roomId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		if (roomId == null)
			return;
		List<UUID> locked = jdbcTemplate.query(
				"""
						select id
						from agenda_room
						where business_id = :businessId
						  and location_id = :locationId
						  and id = :roomId
						  and active = true
						for update
						""", new MapSqlParameterSource().addValue("businessId", businessId)
						.addValue("locationId", locationId).addValue("roomId", roomId),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		if (locked.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la sala indicada.");
		}
		if (hasRoomCapacityConflict(businessId, bookingId, locationId, roomId, startsAt, endsAt)) {
			throw new DataIntegrityViolationException("ROOM_CAPACITY_EXCEEDED");
		}
	}

	private void lockAndAssertProfessionalTravel(UUID businessId, UUID bookingId, UUID professionalId, UUID locationId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		if (professionalId == null)
			return;
		List<UUID> locked = jdbcTemplate.query("""
				select id
				from aesthetic_professional
				where business_id = :businessId
				  and id = :professionalId
				  and active = true
				for update
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), (rs, rowNum) -> rs.getObject("id", UUID.class));
		if (locked.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el profesional indicado.");
		}
		if (hasProfessionalTravelConflict(businessId, bookingId, professionalId, locationId, startsAt, endsAt)) {
			throw new DataIntegrityViolationException("PROFESSIONAL_TRAVEL_TIME_CONFLICT");
		}
	}

	private boolean hasProfessionalConflict(UUID businessId, UUID bookingId, UUID professionalId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("professionalId", professionalId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				  and b.professional_id = :professionalId
				  and b.starts_at < :endsAt
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				""");
		if (bookingId != null) {
			sql.append(" and b.id <> :bookingId\n");
			params.addValue("bookingId", bookingId);
		}
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		return count != null && count > 0;
	}

	private boolean hasProfessionalTravelConflict(UUID businessId, UUID bookingId, UUID professionalId, UUID locationId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("bookingId", bookingId).addValue("professionalId", professionalId)
				.addValue("locationId", locationId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from booking b
				join business_location_travel_time t
				  on t.business_id = b.business_id
				 and t.active = true
				 and (
				      (t.from_location_id = b.location_id and t.to_location_id = :locationId)
				   or (t.from_location_id = :locationId and t.to_location_id = b.location_id)
				 )
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				  and b.professional_id = :professionalId
				  and b.location_id <> :locationId
				  and (cast(:bookingId as uuid) is null or b.id <> :bookingId)
				  and (
				      (
				          coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) <= :startsAt
				          and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval)
				              + (t.travel_minutes || ' minutes')::interval > :startsAt
				      )
				      or (
				          b.starts_at >= :endsAt
				          and :endsAt + (t.travel_minutes || ' minutes')::interval > b.starts_at
				      )
				  )
				""", params, Integer.class);
		return count != null && count > 0;
	}

	public boolean hasBlock(UUID businessId, UUID locationId, UUID professionalId, UUID roomId, OffsetDateTime startsAt,
			OffsetDateTime endsAt) {
		logInput("hasBlock", businessId, locationId, professionalId, roomId, startsAt, endsAt);
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from agenda_block b
				where b.business_id = :businessId
				  and b.active = true
				  and b.starts_at < :endsAt
				  and b.ends_at > :startsAt
				  and (b.location_id is null or b.location_id = :locationId
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("startsAt", startsAt).addValue("endsAt", endsAt);
		if (professionalId != null) {
			sql.append(" or b.professional_id = :professionalId\n");
			params.addValue("professionalId", professionalId);
		}
		if (roomId != null) {
			sql.append(" or b.room_id = :roomId\n");
			params.addValue("roomId", roomId);
		}
		sql.append(")\n");
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		boolean result = count != null && count > 0;
		logOutput("hasBlock", result);
		return result;
	}

	public void recordSlotDiscard(UUID businessId, UUID locationId, UUID serviceId, UUID professionalId, UUID roomId,
			OffsetDateTime slotStartsAt, OffsetDateTime slotEndsAt, OffsetDateTime effectiveStartsAt,
			OffsetDateTime effectiveEndsAt, String reasonCode, String source) {
		if (businessId == null || locationId == null || serviceId == null || slotStartsAt == null || slotEndsAt == null
				|| effectiveStartsAt == null || effectiveEndsAt == null || reasonCode == null || source == null
				|| source.isBlank()) {
			return;
		}
		UUID traceKey = UUID.nameUUIDFromBytes((businessId + "|" + locationId + "|" + serviceId + "|" + professionalId
				+ "|" + roomId + "|" + slotStartsAt + "|" + slotEndsAt + "|" + reasonCode + "|" + source)
				.getBytes(StandardCharsets.UTF_8));
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			log.debug("recordSlotDiscard - tracing skipped in read-only transaction");
			return;
		}
		try {
			jdbcTemplate.update("""
					insert into agenda_slot_discard_trace (
					    id, trace_key, business_id, location_id, service_id, professional_id, room_id,
					    slot_starts_at, slot_ends_at, effective_starts_at, effective_ends_at, reason_code, source,
					    rule_input, result, evaluation_ms
					)
					values (
					    :id, :traceKey, :businessId, :locationId, :serviceId, :professionalId, :roomId,
					    :slotStartsAt, :slotEndsAt, :effectiveStartsAt, :effectiveEndsAt, :reasonCode, :source,
					    jsonb_build_object(
					        'businessId', cast(:businessId as text),
					        'locationId', cast(:locationId as text),
					        'serviceId', cast(:serviceId as text),
					        'professionalId', cast(:professionalId as text),
					        'roomId', cast(:roomId as text),
					        'slotStartsAt', cast(:slotStartsAt as text),
					        'slotEndsAt', cast(:slotEndsAt as text),
					        'effectiveStartsAt', cast(:effectiveStartsAt as text),
					        'effectiveEndsAt', cast(:effectiveEndsAt as text)
					    ),
					    'DISCARDED',
					    0
					)
					on conflict (trace_key) do update
					set occurrence_count = agenda_slot_discard_trace.occurrence_count + 1,
					    rule_input = excluded.rule_input,
					    result = excluded.result,
					    evaluation_ms = excluded.evaluation_ms,
					    updated_at = current_timestamp
					""",
					new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("traceKey", traceKey)
							.addValue("businessId", businessId).addValue("locationId", locationId)
							.addValue("serviceId", serviceId).addValue("professionalId", professionalId)
							.addValue("roomId", roomId).addValue("slotStartsAt", slotStartsAt)
							.addValue("slotEndsAt", slotEndsAt).addValue("effectiveStartsAt", effectiveStartsAt)
							.addValue("effectiveEndsAt", effectiveEndsAt).addValue("reasonCode", reasonCode)
							.addValue("source", source.trim()));
		} catch (DataAccessException ex) {
			log.warn("recordSlotDiscard - tracing skipped - {}", ex.getMostSpecificCause().getMessage());
		}
	}

	public CustomerRecord findOrCreateCustomer(UUID businessId, UUID customerId, String customerName,
			String customerPhone, String customerEmail) {
		logInput("findOrCreateCustomer", businessId, customerId, customerName, customerPhone, customerEmail);
		if (customerId != null) {
			CustomerRecord result = findCustomerById(businessId, customerId);
			logOutput("findOrCreateCustomer", result);
			return result;
		}
		Optional<CustomerRecord> existing = findCustomerByPhone(businessId, customerPhone);
		if (existing.isPresent()) {
			CustomerRecord result = updateExistingCustomerContactIfNeeded(businessId, existing.get(), customerEmail);
			logOutput("findOrCreateCustomer", result);
			return result;
		}
		UUID id = UUID.randomUUID();
		String[] parts = customerName.trim().split("\\s+", 2);
		String firstName = parts[0];
		String lastName = parts.length > 1 ? parts[1] : parts[0];
		jdbcTemplate.update("""
				insert into customer (
				    id, business_id, first_name, last_name, display_name, phone, normalized_phone, email, active
				) values (
				    :id, :businessId, :firstName, :lastName, :displayName, :phone, :normalizedPhone, :email, true
				)
				""",
				new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
						.addValue("firstName", firstName).addValue("lastName", lastName)
						.addValue("displayName", customerName).addValue("phone", customerPhone)
						.addValue("normalizedPhone", customerPhone.replace(" ", "")).addValue("email", customerEmail));
		CustomerRecord result = new CustomerRecord(id, customerName, customerPhone, customerEmail);
		logOutput("findOrCreateCustomer", result);
		return result;
	}

	private CustomerRecord updateExistingCustomerContactIfNeeded(UUID businessId, CustomerRecord customer,
			String customerEmail) {
		String email = normalizeEmail(customerEmail);
		if (email == null || email.equalsIgnoreCase(normalizeEmail(customer.email()))) {
			return customer;
		}
		jdbcTemplate.update("""
				update customer
				set email = :email,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("customerId", customer.id()).addValue("email", email));
		return new CustomerRecord(customer.id(), customer.displayName(), customer.phone(), email);
	}

	private String normalizeEmail(String email) {
		return email == null || email.isBlank() ? null : email.trim();
	}

	public CustomerRecord findCustomerById(UUID businessId, UUID customerId) {
		logInput("findCustomerById", businessId, customerId);
		List<CustomerRecord> items = jdbcTemplate.query("""
				select id, display_name, phone, email
				from customer
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId),
				customerMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el cliente indicado.");
		}
		CustomerRecord result = items.getFirst();
		logOutput("findCustomerById", result);
		return result;
	}

	public Optional<CustomerRecord> findCustomerByPhone(UUID businessId, String customerPhone) {
		logInput("findCustomerByPhone", businessId, customerPhone);
		List<CustomerRecord> items = jdbcTemplate.query("""
				select id, display_name, phone, email
				from customer
				where business_id = :businessId
				  and normalized_phone = :normalizedPhone
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("normalizedPhone",
				customerPhone.replace(" ", "")), customerMapper());
		Optional<CustomerRecord> result = items.stream().findFirst();
		logOutput("findCustomerByPhone", result);
		return result;
	}

	public UUID insertTemporaryBooking(UUID businessId, UUID customerId, UUID conversationId, UUID leadId,
			UUID actorUserId, String subject, UUID locationId, UUID serviceId, UUID professionalId, UUID roomId,
			OffsetDateTime startsAt, OffsetDateTime endsAt, int durationMinutes, OffsetDateTime temporaryExpiresAt,
			boolean requiresDeposit, BigDecimal depositAmount, String notes) {
		logInput("insertTemporaryBooking", businessId, customerId, conversationId, leadId, actorUserId, subject,
				locationId, serviceId, professionalId, roomId, startsAt, endsAt, durationMinutes, temporaryExpiresAt,
				requiresDeposit, depositAmount, notes);
		lockAndAssertLocationDailyCapacity(businessId, null, locationId, startsAt);
		lockAndAssertProfessionalTravel(businessId, null, professionalId, locationId, startsAt, endsAt);
		lockAndAssertRoomCapacity(businessId, null, locationId, roomId, startsAt, endsAt);
		UUID bookingId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into booking (
						    id, business_id, customer_id, lead_id, conversation_id, assigned_user_id, subject, status,
						    starts_at, ends_at, duration_minutes, location_id, location, service_id, professional_id, room_id,
						    temporary_expires_at, source_channel, requires_deposit, deposit_amount, payment_status, notes
						)
						select
						    :id, :businessId, :customerId, :leadId, :conversationId, :actorUserId, :subject, 'PENDIENTE_CONFIRMACION',
						    :startsAt, :endsAt, :durationMinutes, bl.id, bl.name, :serviceId, :professionalId, :roomId,
						    :temporaryExpiresAt, 'WHATSAPP', :requiresDeposit, :depositAmount,
						    case when :requiresDeposit then 'PENDING' else 'NOT_REQUIRED' end,
						    :notes
						from business_location bl
						where bl.business_id = :businessId
						  and bl.id = :locationId
						""",
				new MapSqlParameterSource().addValue("id", bookingId).addValue("businessId", businessId)
						.addValue("customerId", customerId).addValue("leadId", leadId)
						.addValue("conversationId", conversationId).addValue("actorUserId", actorUserId)
						.addValue("subject", subject).addValue("locationId", locationId)
						.addValue("serviceId", serviceId).addValue("professionalId", professionalId)
						.addValue("roomId", roomId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
						.addValue("durationMinutes", durationMinutes).addValue("temporaryExpiresAt", temporaryExpiresAt)
						.addValue("requiresDeposit", requiresDeposit).addValue("depositAmount", depositAmount)
						.addValue("notes", notes));
		insertStatusHistory(businessId, bookingId, null, "PENDIENTE_CONFIRMACION",
				"Reserva pendiente de confirmacion creada desde agenda digital completa.", actorUserId, "WHATSAPP");
		logOutput("insertTemporaryBooking", bookingId);
		return bookingId;
	}

	public void updateBookingSchedule(UUID businessId, UUID bookingId, UUID actorUserId, UUID locationId,
			UUID serviceId, UUID professionalId, UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt,
			int durationMinutes, String reason) {
		logInput("updateBookingSchedule", businessId, bookingId, actorUserId, locationId, serviceId, professionalId,
				roomId, startsAt, endsAt, durationMinutes, reason);
		updateBookingSchedule(businessId, bookingId, actorUserId, locationId, serviceId, professionalId, roomId,
				startsAt, endsAt, durationMinutes, reason, "ADMIN");
		logOutput("updateBookingSchedule", "done");
	}

	public void updateBookingSchedule(UUID businessId, UUID bookingId, UUID actorUserId, UUID locationId,
			UUID serviceId, UUID professionalId, UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt,
			int durationMinutes, String reason, String source) {
		logInput("updateBookingSchedule", businessId, bookingId, actorUserId, locationId, serviceId, professionalId,
				roomId, startsAt, endsAt, durationMinutes, reason, source);
		lockAndAssertLocationDailyCapacity(businessId, bookingId, locationId, startsAt);
		lockAndAssertProfessionalTravel(businessId, bookingId, professionalId, locationId, startsAt, endsAt);
		lockAndAssertRoomCapacity(businessId, bookingId, locationId, roomId, startsAt, endsAt);
		String previousStatus = jdbcTemplate.query("""
				select status from booking where business_id = :businessId and id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				(rs, rowNum) -> rs.getString("status")).stream().findFirst().orElseThrow(
						() -> new ResourceNotFoundException("No se encontro la reserva indicada para reprogramar."));
		int updated = jdbcTemplate.update(
				"""
						update booking
						set location_id = :locationId,
						    location = (select name from business_location where business_id = :businessId and id = :locationId),
						    service_id = :serviceId,
						    professional_id = :professionalId,
						    room_id = :roomId,
						    starts_at = :startsAt,
						    ends_at = :endsAt,
						    duration_minutes = :durationMinutes,
						    status = 'REPROGRAMADA',
						    reschedule_count = reschedule_count + 1,
						    reschedule_reason = :reason,
						    version = version + 1,
						    updated_at = current_timestamp
						where business_id = :businessId
						  and id = :bookingId
						  and status in (:mutableStatuses)
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
						.addValue("locationId", locationId).addValue("serviceId", serviceId)
						.addValue("professionalId", professionalId).addValue("roomId", roomId)
						.addValue("startsAt", startsAt).addValue("endsAt", endsAt)
						.addValue("durationMinutes", durationMinutes).addValue("reason", reason)
						.addValue("mutableStatuses", ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro una reserva activa para reprogramar.");
		}
		insertStatusHistory(businessId, bookingId, previousStatus, "REPROGRAMADA", reason, actorUserId,
				source == null || source.isBlank() ? "ADMIN" : source);
		logOutput("updateBookingSchedule", "done");
	}

	public void cancelBooking(UUID businessId, UUID bookingId, UUID actorUserId, String reason) {
		logInput("cancelBooking", businessId, bookingId, actorUserId, reason);
		cancelBooking(businessId, bookingId, actorUserId, reason, "ADMIN");
		logOutput("cancelBooking", "done");
	}

	public void cancelBooking(UUID businessId, UUID bookingId, UUID actorUserId, String reason, String source) {
		logInput("cancelBooking", businessId, bookingId, actorUserId, reason, source);
		cancelBookingWithStatus(businessId, bookingId, actorUserId, reason, source, "CANCELADA");
		logOutput("cancelBooking", "done");
	}

	public void cancelBookingByCustomer(UUID businessId, UUID bookingId, String reason, String source) {
		logInput("cancelBookingByCustomer", businessId, bookingId, reason, source);
		cancelBookingWithStatus(businessId, bookingId, null, reason, source, "CANCELADA_POR_CLIENTE");
		logOutput("cancelBookingByCustomer", "done");
	}

	private void cancelBookingWithStatus(UUID businessId, UUID bookingId, UUID actorUserId, String reason,
			String source, String targetStatus) {
		String previousStatus = jdbcTemplate.query("""
				select status from booking where business_id = :businessId and id = :bookingId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId),
				(rs, rowNum) -> rs.getString("status")).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro la reserva indicada para cancelar."));
		int updated = jdbcTemplate.update("""
				update booking
				set status = :targetStatus,
				    cancellation_reason = :reason,
				    cancelled_at = coalesce(cancelled_at, current_timestamp),
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :bookingId
				  and status in (:mutableStatuses)
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
						.addValue("reason", reason).addValue("targetStatus", targetStatus)
						.addValue("mutableStatuses", ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro una reserva activa para cancelar.");
		}
		insertStatusHistory(businessId, bookingId, previousStatus, targetStatus, reason, actorUserId,
				source == null || source.isBlank() ? "ADMIN" : source);
		cancelReminderByBooking(businessId, bookingId);
	}

	public void insertStatusHistory(UUID businessId, UUID bookingId, String previousStatus, String newStatus,
			String reason, UUID actorUserId, String source) {
		logInput("insertStatusHistory", businessId, bookingId, previousStatus, newStatus, reason, actorUserId, source);
		jdbcTemplate.update("""
				insert into booking_status_history (
				    id, business_id, booking_id, previous_status, new_status, reason, actor_user_id, source,
				    correlation_id, metadata
				)
				values (
				    :id, :businessId, :bookingId, :previousStatus, :newStatus, :reason, :actorUserId, :source,
				    :correlationId, cast(:metadata as jsonb)
				)
				""", new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
				.addValue("bookingId", bookingId).addValue("previousStatus", previousStatus)
				.addValue("newStatus", newStatus).addValue("reason", reason).addValue("actorUserId", actorUserId)
				.addValue("source", source).addValue("correlationId", CorrelationIdFilter.currentCorrelationId())
				.addValue("metadata", "{}"));
		logOutput("insertStatusHistory", "done");
	}

	public void insertReminder(UUID businessId, UUID bookingId, String type, OffsetDateTime scheduledAt) {
		logInput("insertReminder", businessId, bookingId, type, scheduledAt);
		insertReminder(businessId, bookingId, type, "WHATSAPP", scheduledAt);
		logOutput("insertReminder", "done");
	}

	public void insertReminder(UUID businessId, UUID bookingId, String type, String channelType,
			OffsetDateTime scheduledAt) {
		logInput("insertReminder", businessId, bookingId, type, channelType, scheduledAt);
		if (scheduledAt.isBefore(OffsetDateTime.now())) {
			logOutput("insertReminder", "skipped");
			return;
		}
		jdbcTemplate.update(
				"""
						insert into booking_reminder (id, business_id, booking_id, reminder_type, channel_type, scheduled_at, status, template_key, appointment_revision)
						values (:id, :businessId, :bookingId, :type, :channelType, :scheduledAt, 'PENDING', :templateKey, 0)
						on conflict (business_id, booking_id, reminder_type, channel_type, appointment_revision) do nothing
						""",
				new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
						.addValue("bookingId", bookingId).addValue("type", type).addValue("channelType", channelType)
						.addValue("templateKey", "BOOKING_REMINDER_" + channelType)
						.addValue("scheduledAt", scheduledAt));
		logOutput("insertReminder", "done");
	}

	public void cancelPendingReminders(UUID businessId, UUID bookingId) {
		logInput("cancelPendingReminders", businessId, bookingId);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'CANCELLED', updated_at = current_timestamp
				where business_id = :businessId
				and booking_id = :bookingId
				  and status in ('PENDING', 'SCHEDULED')
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
		logOutput("cancelPendingReminders", "done");
	}

	public List<DueReminderRecord> findDueReminders(OffsetDateTime now, int limit) {
		logInput("findDueReminders", now, limit);
		List<DueReminderRecord> result = jdbcTemplate.query("""
				select
				    br.id,
				    br.business_id,
				    br.booking_id,
				    br.reminder_type,
				    br.channel_type,
				    br.scheduled_at,
				    b.subject,
				    b.status as booking_status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    coalesce(bl.name, b.location) as location_name,
				    s.name as service_name,
				    p.full_name as professional_name,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    c.email as customer_email
				from booking_reminder br
				join booking b on b.id = br.booking_id and b.business_id = br.business_id
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where br.status in ('PENDING', 'SCHEDULED')
				  and br.scheduled_at <= :now
				order by br.scheduled_at asc
				limit :limit
				""", new MapSqlParameterSource().addValue("now", now).addValue("limit", limit),
				(rs, rowNum) -> new DueReminderRecord(rs.getObject("id", UUID.class),
						rs.getObject("business_id", UUID.class), rs.getObject("booking_id", UUID.class),
						rs.getString("reminder_type"), rs.getString("channel_type"),
						rs.getObject("scheduled_at", OffsetDateTime.class), rs.getString("subject"),
						rs.getString("booking_status"), rs.getObject("starts_at", OffsetDateTime.class),
						rs.getObject("ends_at", OffsetDateTime.class), rs.getString("location_name"),
						rs.getString("service_name"), rs.getString("professional_name"), rs.getString("room_name"),
						rs.getString("customer_name"), rs.getString("customer_phone"), rs.getString("customer_email")));
		logOutput("findDueReminders", result);
		return result;
	}

	public void markReminderSent(UUID reminderId, OffsetDateTime sentAt) {
		logInput("markReminderSent", reminderId, sentAt);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'SENT', sent_at = :sentAt, updated_at = current_timestamp
				where id = :reminderId
				""", new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("sentAt", sentAt));
		logOutput("markReminderSent", "done");
	}

	public void markReminderFailed(UUID reminderId, String failureReason) {
		logInput("markReminderFailed", reminderId, failureReason);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'FAILED',
				    error_message = :failureReason,
				    failure_reason = :failureReason,
				    updated_at = current_timestamp
				where id = :reminderId
				""", new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("failureReason",
				failureReason));
		logOutput("markReminderFailed", "done");
	}

	public void markReminderSkipped(UUID reminderId, String failureReason) {
		logInput("markReminderSkipped", reminderId, failureReason);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'SKIPPED',
				    error_message = :failureReason,
				    failure_reason = :failureReason,
				    updated_at = current_timestamp
				where id = :reminderId
				""", new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("failureReason",
				failureReason));
		logOutput("markReminderSkipped", "done");
	}

	public void releaseExpiredTemporaryBookings(OffsetDateTime now) {
		logInput("releaseExpiredTemporaryBookings", now);
		List<ExpiredBookingRecord> expired = jdbcTemplate.query("""
				select id, business_id, status
				from booking
				where status in ('PENDIENTE_CONFIRMACION', 'TEMPORARY', 'SOLICITADA', 'PENDIENTE_PAGO')
				  and temporary_expires_at is not null
				  and temporary_expires_at <= :now
				""", new MapSqlParameterSource().addValue("now", now),
				(rs, rowNum) -> new ExpiredBookingRecord(rs.getObject("id", UUID.class),
						rs.getObject("business_id", UUID.class), rs.getString("status")));
		for (ExpiredBookingRecord booking : expired) {
			jdbcTemplate.update("""
					update booking
					set status = 'EXPIRADA',
					    version = version + 1,
					    updated_at = current_timestamp
					where id = :bookingId
					  and status in ('PENDIENTE_CONFIRMACION', 'TEMPORARY', 'SOLICITADA', 'PENDIENTE_PAGO')
					""", new MapSqlParameterSource().addValue("bookingId", booking.id()));
			jdbcTemplate.update(
					"""
							update booking_confirmation_link
							set status = 'EXPIRED', expired_at = coalesce(expired_at, current_timestamp), updated_at = current_timestamp
							where business_id = :businessId
							  and booking_id = :bookingId
							  and status in ('GENERATED', 'SENT', 'OPENED')
							""",
					new MapSqlParameterSource().addValue("businessId", booking.businessId()).addValue("bookingId",
							booking.id()));
			insertStatusHistory(booking.businessId(), booking.id(), booking.status(), "EXPIRADA",
					"Reserva pendiente expirada y cupo liberado.", null, "SYSTEM");
			cancelPendingReminders(booking.businessId(), booking.id());
		}
		logOutput("releaseExpiredTemporaryBookings", "done");
	}

	public List<AgendaCalendarItemResponse> findCalendar(UUID businessId, OffsetDateTime from, OffsetDateTime to,
			UUID locationId, UUID professionalId, UUID roomId, UUID serviceId, String status) {
		logInput("findCalendar", businessId, from, to, locationId, professionalId, roomId, serviceId, status);
		StringBuilder sql = new StringBuilder("""
				select
				    b.id as booking_id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    b.duration_minutes,
				    b.location_id,
				    coalesce(bl.name, b.location) as location_name,
				    coalesce(bl.timezone, 'America/Santiago') as calendar_timezone,
				    b.service_id,
				    s.name as service_name,
				    b.professional_id,
				    p.full_name as professional_name,
				    b.room_id,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    b.source_channel
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where b.business_id = :businessId
				  and b.starts_at < :to
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :from
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("from", from).addValue("to", to);
		if (locationId != null) {
			sql.append(" and b.location_id = :locationId\n");
			params.addValue("locationId", locationId);
		}
		if (professionalId != null) {
			sql.append(" and b.professional_id = :professionalId\n");
			params.addValue("professionalId", professionalId);
		}
		if (roomId != null) {
			sql.append(" and b.room_id = :roomId\n");
			params.addValue("roomId", roomId);
		}
		if (serviceId != null) {
			sql.append(" and b.service_id = :serviceId\n");
			params.addValue("serviceId", serviceId);
		}
		if (status != null && !status.isBlank()) {
			sql.append(" and b.status = :status\n");
			params.addValue("status", status);
		} else {
			sql.append(" and b.status in (:activeStatuses)\n");
			params.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		}
		sql.append(" order by b.starts_at asc");
		List<AgendaCalendarItemResponse> result = jdbcTemplate.query(sql.toString(), params, calendarMapper());
		logOutput("findCalendar", result);
		return result;
	}

	public List<AgendaCalendarItemResponse> findActiveBookingsByPhone(UUID businessId, String phoneDigits) {
		logInput("findActiveBookingsByPhone", businessId, phoneDigits);
		String sql = """
				select
				    b.id as booking_id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    b.duration_minutes,
				    b.location_id,
				    coalesce(bl.name, b.location) as location_name,
				    coalesce(bl.timezone, 'America/Santiago') as calendar_timezone,
				    b.service_id,
				    s.name as service_name,
				    b.professional_id,
				    p.full_name as professional_name,
				    b.room_id,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    b.source_channel
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				  and b.starts_at >= current_timestamp
				  and regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g') = :phoneDigits
				order by b.starts_at asc limit 10
				""";
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES)
				.addValue("phoneDigits", normalizePhoneDigits(phoneDigits));
		List<AgendaCalendarItemResponse> result = jdbcTemplate.query(sql, params, calendarMapper());
		logOutput("findActiveBookingsByPhone", result);
		return result;
	}

	public List<AgendaCalendarItemResponse> findActiveBookingsForCustomerContext(UUID businessId, UUID customerId,
			UUID conversationId, String normalizedPhone, OffsetDateTime from, OffsetDateTime to, UUID locationId,
			UUID serviceId) {
		logInput("findActiveBookingsForCustomerContext", businessId, customerId, conversationId, normalizedPhone, from,
				to, locationId, serviceId);
		StringBuilder sql = new StringBuilder("""
				select
				    b.id as booking_id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    b.duration_minutes,
				    b.location_id,
				    coalesce(bl.name, b.location) as location_name,
				    coalesce(bl.timezone, 'America/Santiago') as calendar_timezone,
				    b.service_id,
				    s.name as service_name,
				    b.professional_id,
				    p.full_name as professional_name,
				    b.room_id,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    b.source_channel
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				""");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		List<String> orConditions = new ArrayList<>();
		if (customerId != null) {
			orConditions.add("b.customer_id = :customerId");
			params.addValue("customerId", customerId);
		}
		if (conversationId != null) {
			orConditions.add("b.conversation_id = :conversationId");
			params.addValue("conversationId", conversationId);
		}
		String phoneDigits = normalizePhoneDigits(normalizedPhone);
		if (phoneDigits != null) {
			orConditions
					.add("""
							   (regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g') = :phoneDigits
							or right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 9) = :phoneLast9
							or right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 8) = :phoneLast8
							or (:phoneLast4 is not null and right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 4) = :phoneLast4)
							or (:phoneLast4Like is not null and lower(coalesce(c.display_name, '')) like :phoneLast4Like))
							   """
							.stripIndent());
			params.addValue("phoneDigits", phoneDigits);
			params.addValue("phoneLast9", lastDigits(normalizedPhone, 9));
			params.addValue("phoneLast8", lastDigits(normalizedPhone, 8));
			params.addValue("phoneLast4", lastDigits(normalizedPhone, 4));
			params.addValue("phoneLast4Like", "%" + lastDigits(normalizedPhone, 4) + "%");
		}
		if (!orConditions.isEmpty()) {
			sql.append(" and (").append(String.join(" or ", orConditions)).append(")\n");
		}
		if (from != null && to != null) {
			sql.append(" and b.starts_at >= :from and b.starts_at < :to\n");
			params.addValue("from", from, Types.TIMESTAMP_WITH_TIMEZONE);
			params.addValue("to", to, Types.TIMESTAMP_WITH_TIMEZONE);
		}
		if (locationId != null) {
			sql.append(" and b.location_id = :locationId\n");
			params.addValue("locationId", locationId);
		}
		if (serviceId != null) {
			sql.append(" and b.service_id = :serviceId\n");
			params.addValue("serviceId", serviceId);
		}
		sql.append(" order by b.starts_at asc limit 10");
		List<AgendaCalendarItemResponse> result = jdbcTemplate.query(sql.toString(), params, calendarMapper());
		logOutput("findActiveBookingsForCustomerContext", result);
		return result;
	}

	public List<AgendaCalendarItemResponse> findActiveBookingsForOperationalLookup(UUID businessId, OffsetDateTime from,
			OffsetDateTime to, UUID locationId, UUID serviceId, String normalizedPhone, String customerName) {
		logInput("findActiveBookingsForOperationalLookup", businessId, from, to, locationId, serviceId, normalizedPhone,
				customerName);
		StringBuilder sql = new StringBuilder("""
				select
				    b.id as booking_id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    b.duration_minutes,
				    b.location_id,
				    coalesce(bl.name, b.location) as location_name,
				    coalesce(bl.timezone, 'America/Santiago') as calendar_timezone,
				    b.service_id,
				    s.name as service_name,
				    b.professional_id,
				    p.full_name as professional_name,
				    b.room_id,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    b.source_channel
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where b.business_id = :businessId
				  and b.status in (:activeStatuses)
				""");
		String phoneDigits = normalizePhoneDigits(normalizedPhone);
		String phoneLast4 = phoneDigits == null ? null : lastDigits(phoneDigits, 4);
		String customerNameSearch = normalizeText(customerName);
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES).addValue("phoneDigits", phoneDigits)
				.addValue("phoneLast9", lastDigits(normalizedPhone, 9))
				.addValue("phoneLast8", lastDigits(normalizedPhone, 8)).addValue("phoneLast4", phoneLast4)
				.addValue("phoneLast4Like", phoneLast4 == null ? null : "%" + phoneLast4 + "%")
				.addValue("customerNameLike", customerNameSearch == null ? null : "%" + customerNameSearch + "%");
		if (from != null && to != null) {
			sql.append(" and b.starts_at >= :from and b.starts_at < :to\n");
			params.addValue("from", from).addValue("to", to);
		} else {
			sql.append(" and b.starts_at >= current_timestamp - interval '2 hours'\n");
		}
		if (locationId != null) {
			sql.append(" and b.location_id = :locationId\n");
			params.addValue("locationId", locationId);
		}
		if (serviceId != null) {
			sql.append(" and b.service_id = :serviceId\n");
			params.addValue("serviceId", serviceId);
		}
		if (phoneDigits != null || customerNameSearch != null) {
			sql.append(" and (false\n");
			if (phoneDigits != null) {
				sql.append(
						"      or regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g') = :phoneDigits\n")
						.append("      or right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 9) = :phoneLast9\n")
						.append("      or right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 8) = :phoneLast8\n")
						.append("      or (:phoneLast4 is not null and right(regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g'), 4) = :phoneLast4)\n")
						.append("      or (:phoneLast4Like is not null and lower(coalesce(c.display_name, '')) like :phoneLast4Like)\n");
			}
			if (customerNameSearch != null) {
				sql.append("      or lower(coalesce(c.display_name, '')) like :customerNameLike\n")
						.append("      or lower(coalesce(b.subject, '')) like :customerNameLike\n");
			}
			sql.append(" )\n");
		}
		sql.append(" order by b.starts_at asc limit 20");
		List<AgendaCalendarItemResponse> result = jdbcTemplate.query(sql.toString(), params, calendarMapper());
		logOutput("findActiveBookingsForOperationalLookup", result);
		return result;
	}

	private String normalizePhoneDigits(String value) {
		logInput("normalizePhoneDigits", value);
		if (value == null || value.isBlank()) {
			logOutput("normalizePhoneDigits", null);
			return null;
		}
		String digits = value.replaceAll("\\D", "");
		String result = digits.isBlank() ? null : digits;
		logOutput("normalizePhoneDigits", result);
		return result;
	}

	private String normalizeText(String value) {
		logInput("normalizeText", value);
		if (value == null || value.isBlank()) {
			logOutput("normalizeText", null);
			return null;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		String result = normalized.isBlank() ? null : normalized;
		logOutput("normalizeText", result);
		return result;
	}

	private String lastDigits(String value, int size) {
		logInput("lastDigits", value, size);
		String digits = normalizePhoneDigits(value);
		if (digits == null) {
			logOutput("lastDigits", null);
			return null;
		}
		if (digits.length() <= size) {
			logOutput("lastDigits", digits);
			return digits;
		}
		String result = digits.substring(digits.length() - size);
		logOutput("lastDigits", result);
		return result;
	}

	public AgendaBlockResponse insertBlock(UUID businessId, UUID actorUserId, UUID locationId, UUID professionalId,
			UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt, String reason) {
		logInput("insertBlock", businessId, actorUserId, locationId, professionalId, roomId, startsAt, endsAt, reason);
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into agenda_block (id, business_id, location_id, professional_id, room_id, starts_at, ends_at, reason, active, created_by_user_id)
						values (:id, :businessId, :locationId, :professionalId, :roomId, :startsAt, :endsAt, :reason, true, :actorUserId)
						""",
				new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
						.addValue("locationId", locationId).addValue("professionalId", professionalId)
						.addValue("roomId", roomId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
						.addValue("reason", reason).addValue("actorUserId", actorUserId));
		AgendaBlockResponse result = new AgendaBlockResponse(id, locationId, professionalId, roomId, startsAt, endsAt,
				reason, true);
		logOutput("insertBlock", result);
		return result;
	}

	private RowMapper<AgendaFilterOptionResponse> filterOptionMapper() {
		logInput("filterOptionMapper");
		RowMapper<AgendaFilterOptionResponse> result = (rs, rowNum) -> new AgendaFilterOptionResponse(
				rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("detail"),
				rs.getObject("location_id", UUID.class), rs.getBoolean("active"));
		logOutput("filterOptionMapper", "RowMapper<AgendaFilterOptionResponse>");
		return result;
	}

	private RowMapper<TimeWindowRecord> timeWindowMapper() {
		logInput("timeWindowMapper");
		RowMapper<TimeWindowRecord> result = (rs, rowNum) -> new TimeWindowRecord(
				rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class));
		logOutput("timeWindowMapper", "RowMapper<TimeWindowRecord>");
		return result;
	}

	private RowMapper<CustomerRecord> customerMapper() {
		logInput("customerMapper");
		RowMapper<CustomerRecord> result = (rs, rowNum) -> new CustomerRecord(rs.getObject("id", UUID.class),
				rs.getString("display_name"), rs.getString("phone"), rs.getString("email"));
		logOutput("customerMapper", "RowMapper<CustomerRecord>");
		return result;
	}

	private RowMapper<AgendaCalendarItemResponse> calendarMapper() {
		logInput("calendarMapper");
		RowMapper<AgendaCalendarItemResponse> result = (ResultSet rs, int rowNum) -> new AgendaCalendarItemResponse(
				rs.getObject("booking_id", UUID.class), rs.getString("subject"), rs.getString("status"),
				rs.getObject("starts_at", OffsetDateTime.class), rs.getObject("ends_at", OffsetDateTime.class),
				rs.getInt("duration_minutes"), rs.getObject("location_id", UUID.class), rs.getString("location_name"),
				rs.getObject("service_id", UUID.class), rs.getString("service_name"),
				rs.getObject("professional_id", UUID.class), rs.getString("professional_name"),
				rs.getObject("room_id", UUID.class), rs.getString("room_name"), rs.getString("customer_name"),
				rs.getString("customer_phone"), rs.getString("source_channel"),
				formatLocalDateTime(rs.getObject("starts_at", OffsetDateTime.class), rs.getString("calendar_timezone")),
				formatLocalDateTime(rs.getObject("ends_at", OffsetDateTime.class), rs.getString("calendar_timezone")),
				formatLocalDate(rs.getObject("starts_at", OffsetDateTime.class), rs.getString("calendar_timezone")),
				formatLocalTime(rs.getObject("starts_at", OffsetDateTime.class), rs.getString("calendar_timezone")),
				formatLocalTime(rs.getObject("ends_at", OffsetDateTime.class), rs.getString("calendar_timezone")),
				normalizeTimezone(rs.getString("calendar_timezone")), "BOOKING");
		logOutput("calendarMapper", "RowMapper<AgendaCalendarItemResponse>");
		return result;
	}

	private String formatLocalDateTime(OffsetDateTime value, String timezone) {
		logInput("formatLocalDateTime", value, timezone);
		String result = value == null
				? null
				: value.atZoneSameInstant(resolveZone(timezone)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		logOutput("formatLocalDateTime", result);
		return result;
	}

	private String formatLocalDate(OffsetDateTime value, String timezone) {
		logInput("formatLocalDate", value, timezone);
		String result = value == null ? null : value.atZoneSameInstant(resolveZone(timezone)).toLocalDate().toString();
		logOutput("formatLocalDate", result);
		return result;
	}

	private String formatLocalTime(OffsetDateTime value, String timezone) {
		logInput("formatLocalTime", value, timezone);
		String result = value == null
				? null
				: value.atZoneSameInstant(resolveZone(timezone)).toLocalTime()
						.format(DateTimeFormatter.ofPattern("HH:mm"));
		logOutput("formatLocalTime", result);
		return result;
	}

	private String normalizeTimezone(String timezone) {
		logInput("normalizeTimezone", timezone);
		String result = timezone == null || timezone.isBlank() ? "America/Santiago" : timezone.trim();
		logOutput("normalizeTimezone", result);
		return result;
	}

	private ZoneId resolveZone(String timezone) {
		logInput("resolveZone", timezone);
		try {
			ZoneId result = ZoneId.of(normalizeTimezone(timezone));
			logOutput("resolveZone", result);
			return result;
		} catch (RuntimeException ignored) {
			ZoneId result = ZoneId.of("America/Santiago");
			logOutput("resolveZone", result);
			return result;
		}
	}

	private ZoneId resolveBusinessDefaultZone(UUID businessId) {
		List<String> timezones = jdbcTemplate.query(
				"select timezone from business_location where business_id = :businessId and active = true and timezone is not null limit 1",
				new MapSqlParameterSource().addValue("businessId", businessId),
				(rs, rowNum) -> rs.getString("timezone"));
		String zone = timezones.isEmpty() ? null : timezones.getFirst();
		return resolveZone(zone);
	}

	public record LocationRecord(UUID id, String name, String timezone) {
	}

	public record ServiceRecord(UUID id, String name, int durationMinutes, boolean requiresRoom,
			boolean requiresDeposit, BigDecimal depositAmount, int preparationMinutes, int cleanupMinutes,
			boolean active, BigDecimal priceBase, boolean requiresPriorEvaluation, boolean requiresInformedConsent) {

		public ServiceRecord(UUID id, String name, int durationMinutes, boolean requiresRoom, boolean requiresDeposit,
				BigDecimal depositAmount, int preparationMinutes, int cleanupMinutes) {
			this(id, name, durationMinutes, requiresRoom, requiresDeposit, depositAmount, preparationMinutes,
					cleanupMinutes, true, null, false, false);
		}
	}

	public record ProfessionalRecord(UUID id, String name) {
	}

	public record RoomRecord(UUID id, String name, int capacity) {
		public RoomRecord(UUID id, String name) {
			this(id, name, 1);
		}
	}

	public record TimeWindowRecord(LocalTime startTime, LocalTime endTime) {
	}

	public record CustomerRecord(UUID id, String displayName, String phone, String email) {
	}

	private record ExpiredBookingRecord(UUID id, UUID businessId, String status) {
	}

	public boolean hasActiveAbsence(UUID businessId, UUID professionalId, OffsetDateTime startsAt,
			OffsetDateTime endsAt) {
		logInput("hasActiveAbsence", businessId, professionalId, startsAt, endsAt);
		if (professionalId == null) {
			logOutput("hasActiveAbsence", false);
			return false;
		}
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from professional_absence a
				where a.business_id = :businessId
				  and a.professional_id = :professionalId
				  and a.active = true
				  and a.starts_at < :endsAt
				  and a.ends_at > :startsAt
				""", new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("professionalId", professionalId).addValue("startsAt", startsAt).addValue("endsAt", endsAt),
				Integer.class);
		boolean result = count != null && count > 0;
		logOutput("hasActiveAbsence", result);
		return result;
	}

	public Integer findProfessionalMaxDailyBookings(UUID businessId, UUID professionalId) {
		logInput("findProfessionalMaxDailyBookings", businessId, professionalId);
		if (professionalId == null) {
			logOutput("findProfessionalMaxDailyBookings", null);
			return null;
		}
		Integer result = jdbcTemplate.query("""
				select max_daily_bookings
				from aesthetic_professional
				where business_id = :businessId
				  and id = :professionalId
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), (rs) -> rs.next() ? rs.getObject("max_daily_bookings", Integer.class) : null);
		logOutput("findProfessionalMaxDailyBookings", result);
		return result;
	}

	public int countProfessionalBookingsOnDate(UUID businessId, UUID professionalId, OffsetDateTime date) {
		logInput("countProfessionalBookingsOnDate", businessId, professionalId, date);
		if (professionalId == null) {
			logOutput("countProfessionalBookingsOnDate", 0);
			return 0;
		}
		ZoneId zone = resolveBusinessDefaultZone(businessId);
		OffsetDateTime dayStart = date.atZoneSameInstant(zone).toLocalDate().atStartOfDay(zone).toOffsetDateTime();
		OffsetDateTime dayEnd = dayStart.plusDays(1);
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.professional_id = :professionalId
				  and b.starts_at >= :dayStart
				  and b.starts_at < :dayEnd
				  and b.status not in ('CANCELADA', 'EXPIRADA', 'NO_ASISTE')
				""", new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("professionalId", professionalId).addValue("dayStart", dayStart).addValue("dayEnd", dayEnd),
				Integer.class);
		int result = count != null ? count : 0;
		logOutput("countProfessionalBookingsOnDate", result);
		return result;
	}

	public int countCustomerActiveOverlappingBookingsExcluding(UUID businessId, UUID customerId, UUID professionalId,
			OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeBookingId) {
		logInput("countCustomerActiveOverlappingBookingsExcluding", businessId, customerId, professionalId, startsAt,
				endsAt, excludeBookingId);
		if (customerId == null) {
			logOutput("countCustomerActiveOverlappingBookingsExcluding", 0);
			return 0;
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("customerId", customerId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.customer_id = :customerId
				  and b.status in (:activeStatuses)
				  and b.starts_at < :endsAt
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				""");
		if (professionalId != null) {
			sql.append(" and b.professional_id = :professionalId\n");
			params.addValue("professionalId", professionalId);
		}
		if (excludeBookingId != null) {
			sql.append(" and b.id <> :excludeBookingId\n");
			params.addValue("excludeBookingId", excludeBookingId);
		}
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		int result = count != null ? count : 0;
		logOutput("countCustomerActiveOverlappingBookingsExcluding", result);
		return result;
	}

	public UUID findActiveBookingByCustomerProfessionalAndStart(UUID businessId, UUID customerId, UUID professionalId,
			OffsetDateTime startsAt) {
		logInput("findActiveBookingByCustomerProfessionalAndStart", businessId, customerId, professionalId, startsAt);
		if (customerId == null || professionalId == null || startsAt == null) {
			logOutput("findActiveBookingByCustomerProfessionalAndStart", null);
			return null;
		}
		List<UUID> items = jdbcTemplate.query("""
				select id from booking
				where business_id = :businessId
				  and customer_id = :customerId
				  and professional_id = :professionalId
				  and starts_at = :startsAt
				  and status in (:activeStatuses)
				order by created_at desc limit 1
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId)
						.addValue("professionalId", professionalId).addValue("startsAt", startsAt)
						.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		UUID result = items.isEmpty() ? null : items.getFirst();
		logOutput("findActiveBookingByCustomerProfessionalAndStart", result);
		return result;
	}

	public int countCustomerActiveOverlappingBookings(UUID businessId, UUID customerId, UUID professionalId,
			OffsetDateTime startsAt, OffsetDateTime endsAt) {
		logInput("countCustomerActiveOverlappingBookings", businessId, customerId, professionalId, startsAt, endsAt);
		if (customerId == null) {
			logOutput("countCustomerActiveOverlappingBookings", 0);
			return 0;
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("customerId", customerId).addValue("startsAt", startsAt).addValue("endsAt", endsAt)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.customer_id = :customerId
				  and b.status in (:activeStatuses)
				  and b.starts_at < :endsAt
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :startsAt
				""");
		if (professionalId != null) {
			sql.append(" and b.professional_id = :professionalId\n");
			params.addValue("professionalId", professionalId);
		}
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		int result = count != null ? count : 0;
		logOutput("countCustomerActiveOverlappingBookings", result);
		return result;
	}

	public Optional<UUID> findLastCustomerLocationId(UUID businessId, String phoneDigits) {
		logInput("findLastCustomerLocationId", businessId, phoneDigits);
		List<UUID> items = jdbcTemplate.query("""
				select b.location_id
				from booking b
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				where b.business_id = :businessId
				  and regexp_replace(coalesce(c.normalized_phone, c.phone, ''), '\\D', '', 'g') = :phoneDigits
				  and b.location_id is not null
				order by b.starts_at desc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("phoneDigits",
				normalizePhoneDigits(phoneDigits)), (rs, rowNum) -> rs.getObject("location_id", UUID.class));
		Optional<UUID> result = items.stream().findFirst();
		logOutput("findLastCustomerLocationId", result);
		return result;
	}

	public int findMinAdvanceNoticeMinutes(UUID businessId, UUID locationId, int dayOfWeek, UUID professionalId) {
		logInput("findMinAdvanceNoticeMinutes", businessId, locationId, dayOfWeek, professionalId);
		// Professional-specific min advance takes precedence
		if (professionalId != null) {
			List<Integer> profResults = jdbcTemplate.query("""
					select ph.min_advance_notice_minutes
					from agenda_professional_hours ph
					where ph.business_id = :businessId
					  and ph.location_id = :locationId
					  and ph.professional_id = :professionalId
					  and ph.day_of_week = :dayOfWeek
					  and ph.active = true
					limit 1
					""",
					new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
							.addValue("professionalId", professionalId).addValue("dayOfWeek", dayOfWeek),
					(rs, rowNum) -> rs.getInt("min_advance_notice_minutes"));
			if (!profResults.isEmpty()) {
				int profMinAdvance = profResults.get(0);
				logOutput("findMinAdvanceNoticeMinutes", profMinAdvance);
				return profMinAdvance;
			}
		}
		// Fallback to business hours min advance
		List<Integer> bizResults = jdbcTemplate.query(
				"""
						select min_advance_notice_minutes
						from agenda_business_hours bh
						where bh.business_id = :businessId
						  and bh.location_id = :locationId
						  and bh.day_of_week = :dayOfWeek
						  and bh.active = true
						limit 1
						""", new MapSqlParameterSource().addValue("businessId", businessId)
						.addValue("locationId", locationId).addValue("dayOfWeek", dayOfWeek),
				(rs, rowNum) -> rs.getInt("min_advance_notice_minutes"));
		int result = bizResults.isEmpty() ? 60 : bizResults.get(0);
		logOutput("findMinAdvanceNoticeMinutes", result);
		return result;
	}

	public UUID findUserDefaultLocation(UUID businessId, UUID userId) {
		List<UUID> items = jdbcTemplate.query("""
				select location_id from business_user
				where business_id = :businessId and user_id = :userId and active = true
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("userId", userId),
				(rs, rowNum) -> rs.getObject("location_id", UUID.class));
		return items.isEmpty() ? null : items.getFirst();
	}

	public UUID findFirstLocation(UUID businessId) {
		List<UUID> items = jdbcTemplate.query("""
				select id from business_location
				where business_id = :businessId and active = true
				order by name limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		return items.isEmpty() ? null : items.getFirst();
	}

	public List<BusinessHoursRecord> findAllBusinessHours(UUID businessId, UUID locationId) {
		logInput("findAllBusinessHours", businessId, locationId);
		List<BusinessHoursRecord> result = jdbcTemplate.query("""
				select day_of_week, start_time, end_time
				from agenda_business_hours
				where business_id = :businessId
				  and location_id = :locationId
				  and active = true
				order by day_of_week, start_time
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
				(rs, rowNum) -> new BusinessHoursRecord(rs.getInt("day_of_week"),
						rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class)));
		logOutput("findAllBusinessHours", result);
		return result;
	}

	public record BusinessHoursRecord(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
	}

	public void replaceBusinessHours(UUID businessId, UUID locationId, List<BusinessHoursRecord> hours) {
		logInput("replaceBusinessHours", businessId, locationId, hours.size());
		jdbcTemplate.update(
				"delete from agenda_business_hours where business_id = :businessId and location_id = :locationId",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId));
		if (!hours.isEmpty()) {
			MapSqlParameterSource[] batch = hours.stream()
					.map(h -> new MapSqlParameterSource().addValue("id", UUID.randomUUID())
							.addValue("businessId", businessId).addValue("locationId", locationId)
							.addValue("dayOfWeek", h.dayOfWeek()).addValue("startTime", h.startTime())
							.addValue("endTime", h.endTime()))
					.toArray(MapSqlParameterSource[]::new);
			jdbcTemplate.batchUpdate(
					"""
							insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
							values (:id, :businessId, :locationId, :dayOfWeek, :startTime, :endTime, true)
							""",
					batch);
		}
		logOutput("replaceBusinessHours", "done");
	}

	public void replaceProfessionalHours(UUID businessId, UUID locationId, UUID professionalId,
			List<BusinessHoursRecord> hours) {
		logInput("replaceProfessionalHours", businessId, locationId, professionalId, hours.size());
		jdbcTemplate.update("""
				delete from agenda_professional_hours
				where business_id = :businessId and location_id = :locationId and professional_id = :professionalId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("professionalId", professionalId));
		if (!hours.isEmpty()) {
			MapSqlParameterSource[] batch = hours.stream()
					.map(h -> new MapSqlParameterSource().addValue("id", UUID.randomUUID())
							.addValue("businessId", businessId).addValue("locationId", locationId)
							.addValue("professionalId", professionalId).addValue("dayOfWeek", h.dayOfWeek())
							.addValue("startTime", h.startTime()).addValue("endTime", h.endTime()))
					.toArray(MapSqlParameterSource[]::new);
			jdbcTemplate.batchUpdate(
					"""
							insert into agenda_professional_hours (id, business_id, location_id, professional_id, day_of_week, start_time, end_time, active)
							values (:id, :businessId, :locationId, :professionalId, :dayOfWeek, :startTime, :endTime, true)
							""",
					batch);
		}
		logOutput("replaceProfessionalHours", "done");
	}

	public List<DueReminderRecord> claimDueReminders(OffsetDateTime now, int limit, String instanceId) {
		logInput("claimDueReminders", now, limit, instanceId);
		List<DueReminderRecord> result = jdbcTemplate.query("""
				select
				    br.id,
				    br.business_id,
				    br.booking_id,
				    br.reminder_type,
				    br.channel_type,
				    br.scheduled_at,
				    b.subject,
				    b.status as booking_status,
				    b.starts_at,
				    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
				    coalesce(bl.name, b.location) as location_name,
				    s.name as service_name,
				    p.full_name as professional_name,
				    r.name as room_name,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    c.email as customer_email
				from booking_reminder br
				join booking b on b.id = br.booking_id and b.business_id = br.business_id
				join customer c on c.id = b.customer_id and c.business_id = b.business_id
				left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
				left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
				left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
				where br.id in (
				    select id from booking_reminder
				    where status in ('PENDING', 'RETRY')
				      and scheduled_at <= :now
				      and (next_attempt_at is null or next_attempt_at <= :now)
				    order by scheduled_at asc
				    limit :limit
				    for update skip locked
				)
				""", new MapSqlParameterSource().addValue("now", now).addValue("limit", limit),
				(rs, rowNum) -> new DueReminderRecord(rs.getObject("id", UUID.class),
						rs.getObject("business_id", UUID.class), rs.getObject("booking_id", UUID.class),
						rs.getString("reminder_type"), rs.getString("channel_type"),
						rs.getObject("scheduled_at", OffsetDateTime.class), rs.getString("subject"),
						rs.getString("booking_status"), rs.getObject("starts_at", OffsetDateTime.class),
						rs.getObject("ends_at", OffsetDateTime.class), rs.getString("location_name"),
						rs.getString("service_name"), rs.getString("professional_name"), rs.getString("room_name"),
						rs.getString("customer_name"), rs.getString("customer_phone"), rs.getString("customer_email")));
		if (!result.isEmpty()) {
			markRemindersProcessing(result, instanceId);
		}
		logOutput("claimDueReminders", result);
		return result;
	}

	private void markRemindersProcessing(List<DueReminderRecord> reminders, String instanceId) {
		SqlParameterSource[] batch = reminders.stream()
				.map(r -> new MapSqlParameterSource().addValue("id", UUID.randomUUID())
						.addValue("processingStartedAt", OffsetDateTime.now(ZoneOffset.UTC))
						.addValue("instanceId", instanceId).addValue("reminderId", r.id()))
				.toArray(SqlParameterSource[]::new);
		jdbcTemplate.batchUpdate("""
				update booking_reminder
				set status = 'PROCESSING',
				    processing_started_at = :processingStartedAt,
				    processing_instance = :instanceId,
				    updated_at = current_timestamp
				where id = :reminderId and status in ('PENDING', 'RETRY')
				""", batch);
	}

	public void markReminderProcessing(UUID reminderId, String instanceId) {
		logInput("markReminderProcessing", reminderId, instanceId);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'PROCESSING',
				    processing_started_at = current_timestamp,
				    processing_instance = :instanceId,
				    attempt_count = attempt_count + 1,
				    updated_at = current_timestamp
				where id = :reminderId and status in ('PENDING', 'RETRY')
				""", new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("instanceId", instanceId));
		logOutput("markReminderProcessing", "done");
	}

	public void markReminderRetry(UUID reminderId, OffsetDateTime nextAttemptAt, String errorCode,
			String errorMessage) {
		logInput("markReminderRetry", reminderId, nextAttemptAt, errorCode, errorMessage);
		String truncated = errorMessage != null && errorMessage.length() > 500
				? errorMessage.substring(0, 500)
				: errorMessage;
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'RETRY',
				    next_attempt_at = :nextAttemptAt,
				    last_error_code = :errorCode,
				    error_message = :errorMessage,
				    failure_reason = :errorMessage,
				    processing_started_at = null,
				    processing_instance = null,
				    updated_at = current_timestamp
				where id = :reminderId and status = 'PROCESSING'
				""",
				new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("nextAttemptAt", nextAttemptAt)
						.addValue("errorCode", errorCode).addValue("errorMessage", truncated));
		logOutput("markReminderRetry", "done");
	}

	public void markReminderSentWithProvider(UUID reminderId, OffsetDateTime sentAt, String providerMessageId) {
		logInput("markReminderSentWithProvider", reminderId, sentAt, providerMessageId);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'SENT',
				    sent_at = :sentAt,
				    provider_message_id = :providerMessageId,
				    processing_started_at = null,
				    processing_instance = null,
				    updated_at = current_timestamp
				where id = :reminderId and status = 'PROCESSING'
				""", new MapSqlParameterSource().addValue("reminderId", reminderId).addValue("sentAt", sentAt)
				.addValue("providerMessageId", providerMessageId));
		logOutput("markReminderSentWithProvider", "done");
	}

	public int recoverStaleProcessingReminders(OffsetDateTime timeoutThreshold, String instanceId) {
		logInput("recoverStaleProcessingReminders", timeoutThreshold, instanceId);
		int updated = jdbcTemplate.update("""
				update booking_reminder
				set status = 'RETRY',
				    last_error_code = 'RECOVERY_TIMEOUT',
				    error_message = 'Processing timeout exceeded, recovered by ' || :instanceId,
				    failure_reason = 'Processing timeout exceeded',
				    processing_started_at = null,
				    processing_instance = null,
				    updated_at = current_timestamp
				where status = 'PROCESSING'
				  and processing_started_at < :timeoutThreshold
				""", new MapSqlParameterSource().addValue("timeoutThreshold", timeoutThreshold).addValue("instanceId",
				instanceId));
		if (updated > 0) {
			logOutput("recoverStaleProcessingReminders", "recovered=" + updated);
		}
		return updated;
	}

	public void cancelReminderByBooking(UUID businessId, UUID bookingId) {
		logInput("cancelReminderByBooking", businessId, bookingId);
		jdbcTemplate.update("""
				update booking_reminder
				set status = 'CANCELLED', updated_at = current_timestamp
				where business_id = :businessId
				  and booking_id = :bookingId
				  and status in ('PENDING', 'RETRY', 'SCHEDULED')
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
		logOutput("cancelReminderByBooking", "done");
	}

	public Integer findMaxReminderRevision(UUID businessId, UUID bookingId, String reminderType, String channelType) {
		try {
			return jdbcTemplate.queryForObject("""
					select max(appointment_revision) from booking_reminder
					where business_id = :businessId
					  and booking_id = :bookingId
					  and reminder_type = :reminderType
					  and channel_type = :channelType
					""",
					new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId)
							.addValue("reminderType", reminderType).addValue("channelType", channelType),
					Integer.class);
		} catch (Exception e) {
			return null;
		}
	}

	public void insertReminderWithRevision(UUID businessId, UUID bookingId, String type, String channelType,
			OffsetDateTime scheduledAt, int revision) {
		logInput("insertReminderWithRevision", businessId, bookingId, type, channelType, scheduledAt, revision);
		if (scheduledAt == null) {
			logOutput("insertReminderWithRevision", "skipped null scheduledAt");
			return;
		}
		jdbcTemplate.update("""
				insert into booking_reminder (id, business_id, booking_id, reminder_type, channel_type,
				    scheduled_at, status, template_key, appointment_revision)
				values (:id, :businessId, :bookingId, :type, :channelType,
				    :scheduledAt, 'PENDING', :templateKey, :revision)
				on conflict (business_id, booking_id, reminder_type, channel_type, appointment_revision)
				do nothing
				""",
				new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
						.addValue("bookingId", bookingId).addValue("type", type).addValue("channelType", channelType)
						.addValue("scheduledAt", scheduledAt).addValue("templateKey", "BOOKING_REMINDER_" + channelType)
						.addValue("revision", revision));
		logOutput("insertReminderWithRevision", "done");
	}

	public record DueReminderRecord(UUID id, UUID businessId, UUID bookingId, String reminderType, String channelType,
			OffsetDateTime scheduledAt, String subject, String bookingStatus, OffsetDateTime startsAt,
			OffsetDateTime endsAt, String locationName, String serviceName, String professionalName, String roomName,
			String customerName, String customerPhone, String customerEmail) {
	}

	public record BookingOperationIdempotencyRecord(String operationType, String idempotencyKey, String requestHash,
			String status, UUID bookingId) {
	}
}
