package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.ProfessionalRequest;
import com.asistentewhatsapp.administration.api.ProfessionalResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfessionalJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ProfessionalJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public PagedResponse<ProfessionalResponse> findProfessionals(UUID businessId, int page, int size, String search,
			Boolean active) {
		var sql = new StringBuilder("""
				from aesthetic_professional ap
				where ap.business_id = :businessId
				""");
		var params = new MapSqlParameterSource().addValue("businessId", businessId);

		if (search != null) {
			sql.append("""
					and (ap.full_name ilike :search
					     or ap.display_name ilike :search
					     or ap.specialty ilike :search
					     or coalesce(ap.email, '') ilike :search)
					""");
			params.addValue("search", "%" + search + "%");
		}

		if (active != null) {
			sql.append(" and ap.active = :active ");
			params.addValue("active", active);
		}

		Long totalItems = namedParameterJdbcTemplate.queryForObject("select count(*) " + sql.toString(), params,
				Long.class);
		long resolvedTotal = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotal == 0 ? 0 : (int) Math.ceil((double) resolvedTotal / size);

		params.addValue("limit", size).addValue("offset", (long) page * size);

		List<ProfessionalResponse> items = namedParameterJdbcTemplate.query("""
				select ap.id, ap.full_name, ap.display_name, ap.specialty, ap.email, ap.phone,
				       ap.description, ap.color, ap.max_daily_bookings, ap.qualification_level,
				       ap.certification_ref, ap.certification_valid_until, ap.active,
				       ap.created_at, ap.updated_at
				""" + sql.toString() + """
				order by ap.display_name asc, ap.full_name asc
				limit :limit offset :offset
				""", params, professionalRowMapper());

		List<ProfessionalResponse> enriched = items.stream()
				.map(p -> new ProfessionalResponse(p.id(), p.fullName(), p.displayName(), p.specialty(), p.email(),
						p.phone(), p.description(), p.color(), p.maxDailyBookings(), p.qualificationLevel(),
						p.certificationRef(), p.certificationValidUntil(), p.active(),
						findLocationIds(businessId, p.id()), findLocationNames(businessId, p.id()), p.createdAt(),
						p.updatedAt()))
				.toList();

		return new PagedResponse<>(enriched, page, size, resolvedTotal, totalPages);
	}

	public ProfessionalResponse findProfessional(UUID businessId, UUID professionalId) {
		List<ProfessionalResponse> items = namedParameterJdbcTemplate.query("""
				select ap.id, ap.full_name, ap.display_name, ap.specialty, ap.email, ap.phone,
				       ap.description, ap.color, ap.max_daily_bookings, ap.qualification_level,
				       ap.certification_ref, ap.certification_valid_until, ap.active,
				       ap.created_at, ap.updated_at
				from aesthetic_professional ap
				where ap.business_id = :businessId
				  and ap.id = :professionalId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), professionalRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el profesional solicitado.");
		}
		ProfessionalResponse p = items.getFirst();
		return new ProfessionalResponse(p.id(), p.fullName(), p.displayName(), p.specialty(), p.email(), p.phone(),
				p.description(), p.color(), p.maxDailyBookings(), p.qualificationLevel(), p.certificationRef(),
				p.certificationValidUntil(), p.active(), findLocationIds(businessId, p.id()),
				findLocationNames(businessId, p.id()), p.createdAt(), p.updatedAt());
	}

	public ProfessionalResponse insertProfessional(UUID businessId, ProfessionalRequest request) {
		UUID professionalId = UUID.randomUUID();
		String fullName = request.fullName().trim();
		String displayName = request.displayName() != null && !request.displayName().isBlank()
				? request.displayName().trim()
				: fullName;
		String specialty = request.specialty() != null && !request.specialty().isBlank()
				? request.specialty().trim()
				: "";
		String email = blankToNull(request.email());
		String phone = blankToNull(request.phone());
		String description = blankToNull(request.description());
		String color = blankToNull(request.color());
		Integer maxDailyBookings = request.maxDailyBookings();
		Integer qualificationLevel = request.qualificationLevel();
		String certificationRef = blankToNull(request.certificationRef());
		boolean active = request.active() == null || request.active();
		List<UUID> locationIds = request.locationIds() != null ? request.locationIds() : List.of();

		var params = new MapSqlParameterSource().addValue("id", professionalId).addValue("businessId", businessId)
				.addValue("fullName", fullName).addValue("displayName", displayName).addValue("specialty", specialty)
				.addValue("email", email).addValue("phone", phone).addValue("description", description)
				.addValue("color", color).addValue("maxDailyBookings", maxDailyBookings)
				.addValue("qualificationLevel", qualificationLevel).addValue("certificationRef", certificationRef)
				.addValue("active", active);

		try {
			namedParameterJdbcTemplate.update("""
					insert into aesthetic_professional (
					    id, business_id, full_name, display_name, specialty, email, phone,
					    description, color, max_daily_bookings, qualification_level,
					    certification_ref, active
					) values (
					    :id, :businessId, :fullName, :displayName, :specialty, :email, :phone,
					    :description, :color, :maxDailyBookings, :qualificationLevel,
					    :certificationRef, :active
					)
					""", params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe un profesional con ese nombre en la empresa.",
					Map.of("fullName", "El nombre ya esta registrado."));
		}

		updateLocations(businessId, professionalId, locationIds);
		return findProfessional(businessId, professionalId);
	}

	public ProfessionalResponse updateProfessional(UUID businessId, UUID professionalId, ProfessionalRequest request) {
		String fullName = request.fullName().trim();
		String displayName = request.displayName() != null && !request.displayName().isBlank()
				? request.displayName().trim()
				: fullName;
		String specialty = request.specialty() != null && !request.specialty().isBlank()
				? request.specialty().trim()
				: "";
		String email = blankToNull(request.email());
		String phone = blankToNull(request.phone());
		String description = blankToNull(request.description());
		String color = blankToNull(request.color());
		Integer maxDailyBookings = request.maxDailyBookings();
		Integer qualificationLevel = request.qualificationLevel();
		String certificationRef = blankToNull(request.certificationRef());
		boolean active = request.active() == null || request.active();
		List<UUID> locationIds = request.locationIds() != null ? request.locationIds() : List.of();

		var params = new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", professionalId)
				.addValue("fullName", fullName).addValue("displayName", displayName).addValue("specialty", specialty)
				.addValue("email", email).addValue("phone", phone).addValue("description", description)
				.addValue("color", color).addValue("maxDailyBookings", maxDailyBookings)
				.addValue("qualificationLevel", qualificationLevel).addValue("certificationRef", certificationRef)
				.addValue("active", active);

		try {
			int updated = namedParameterJdbcTemplate.update("""
					update aesthetic_professional
					set full_name = :fullName,
					    display_name = :displayName,
					    specialty = :specialty,
					    email = :email,
					    phone = :phone,
					    description = :description,
					    color = :color,
					    max_daily_bookings = :maxDailyBookings,
					    qualification_level = :qualificationLevel,
					    certification_ref = :certificationRef,
					    active = :active,
					    updated_at = current_timestamp
					where business_id = :businessId
					  and id = :id
					""", params);
			if (updated == 0) {
				throw new ResourceNotFoundException("No se encontro el profesional solicitado.");
			}
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe un profesional con ese nombre en la empresa.",
					Map.of("fullName", "El nombre ya esta registrado."));
		}

		updateLocations(businessId, professionalId, locationIds);
		return findProfessional(businessId, professionalId);
	}

	private void updateLocations(UUID businessId, UUID professionalId, List<UUID> locationIds) {
		namedParameterJdbcTemplate.update("""
				delete from aesthetic_professional_location
				where business_id = :businessId
				  and professional_id = :professionalId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId));

		if (locationIds.isEmpty()) {
			return;
		}

		var batchParams = new MapSqlParameterSource[locationIds.size()];
		for (int i = 0; i < locationIds.size(); i++) {
			batchParams[i] = new MapSqlParameterSource().addValue("id", UUID.randomUUID())
					.addValue("businessId", businessId).addValue("professionalId", professionalId)
					.addValue("locationId", locationIds.get(i));
		}

		namedParameterJdbcTemplate.batchUpdate("""
				insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
				values (:id, :businessId, :professionalId, :locationId, true)
				on conflict (business_id, professional_id, location_id) do nothing
				""", batchParams);
	}

	private List<UUID> findLocationIds(UUID businessId, UUID professionalId) {
		return namedParameterJdbcTemplate.queryForList("""
				select location_id
				from aesthetic_professional_location
				where business_id = :businessId
				  and professional_id = :professionalId
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), UUID.class);
	}

	private List<String> findLocationNames(UUID businessId, UUID professionalId) {
		return namedParameterJdbcTemplate.queryForList("""
				select bl.name
				from aesthetic_professional_location apl
				join business_location bl on bl.id = apl.location_id
				where apl.business_id = :businessId
				  and apl.professional_id = :professionalId
				  and apl.active = true
				order by bl.name
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
				professionalId), String.class);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private RowMapper<ProfessionalResponse> professionalRowMapper() {
		return (ResultSet rs, int rowNum) -> {
			UUID id = rs.getObject("id", UUID.class);
			String fullName = rs.getString("full_name");
			String displayName = rs.getString("display_name");
			String specialty = rs.getString("specialty");
			String email = rs.getString("email");
			String phone = rs.getString("phone");
			String description = rs.getString("description");
			String color = rs.getString("color");
			int maxDb = rs.getInt("max_daily_bookings");
			Integer maxDailyBookings = rs.wasNull() ? null : maxDb;
			int ql = rs.getInt("qualification_level");
			Integer qualificationLevel = rs.wasNull() ? null : ql;
			String certificationRef = rs.getString("certification_ref");
			LocalDate certificationValidUntil = rs.getObject("certification_valid_until", LocalDate.class);
			boolean active = rs.getBoolean("active");
			OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
			OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
			return new ProfessionalResponse(id, fullName, displayName, specialty, email, phone, description, color,
					maxDailyBookings, qualificationLevel, certificationRef, certificationValidUntil, active, List.of(),
					List.of(), createdAt, updatedAt);
		};
	}
}
