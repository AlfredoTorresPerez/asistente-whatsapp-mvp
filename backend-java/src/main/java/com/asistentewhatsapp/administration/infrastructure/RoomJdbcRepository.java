package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.RoomRequest;
import com.asistentewhatsapp.administration.api.RoomResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
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
public class RoomJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public RoomJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public PagedResponse<RoomResponse> findRooms(UUID businessId, int page, int size, String search, UUID locationId,
			String roomType, Boolean active) {
		var sql = new StringBuilder("""
				from agenda_room ar
				join business_location bl on bl.id = ar.location_id
				where ar.business_id = :businessId
				""");
		var params = new MapSqlParameterSource().addValue("businessId", businessId);

		if (search != null) {
			sql.append("""
					and (ar.name ilike :search
					     or ar.code ilike :search
					     or coalesce(ar.description, '') ilike :search)
					""");
			params.addValue("search", "%" + search + "%");
		}

		if (locationId != null) {
			sql.append(" and ar.location_id = :locationId ");
			params.addValue("locationId", locationId);
		}

		if (roomType != null) {
			sql.append(" and ar.room_type = :roomType ");
			params.addValue("roomType", roomType);
		}

		if (active != null) {
			sql.append(" and ar.active = :active ");
			params.addValue("active", active);
		}

		Long totalItems = namedParameterJdbcTemplate.queryForObject("select count(*) " + sql.toString(), params,
				Long.class);
		long resolvedTotal = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotal == 0 ? 0 : (int) Math.ceil((double) resolvedTotal / size);

		params.addValue("limit", size).addValue("offset", (long) page * size);

		List<RoomResponse> items = namedParameterJdbcTemplate.query("""
				select ar.id, ar.location_id, bl.name as location_name, ar.code, ar.name,
				       ar.room_type, ar.capacity, ar.description, ar.color, ar.notes,
				       ar.active, ar.created_at, ar.updated_at
				""" + sql.toString() + """
				order by bl.name, ar.name
				limit :limit offset :offset
				""", params, roomRowMapper());

		return new PagedResponse<>(items, page, size, resolvedTotal, totalPages);
	}

	public RoomResponse findRoom(UUID businessId, UUID roomId) {
		List<RoomResponse> items = namedParameterJdbcTemplate.query("""
				select ar.id, ar.location_id, bl.name as location_name, ar.code, ar.name,
				       ar.room_type, ar.capacity, ar.description, ar.color, ar.notes,
				       ar.active, ar.created_at, ar.updated_at
				from agenda_room ar
				join business_location bl on bl.id = ar.location_id
				where ar.business_id = :businessId
				  and ar.id = :roomId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("roomId", roomId),
				roomRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la cabina o recurso solicitado.");
		}
		return items.getFirst();
	}

	public RoomResponse insertRoom(UUID businessId, RoomRequest request) {
		UUID roomId = UUID.randomUUID();
		List<UUID> locationIds = request.effectiveLocationIds();
		if (locationIds.isEmpty()) {
			throw new ConflictException("La sede es obligatoria.",
					Map.of("locationId", "Selecciona una sede para la cabina."));
		}
		UUID locationId = locationIds.getFirst();
		String code = request.code().trim().toLowerCase();
		String name = request.name().trim();
		String roomType = request.roomType().trim().toUpperCase();
		int capacity = request.capacity() != null && request.capacity() > 0 ? request.capacity() : 1;
		String description = blankToNull(request.description());
		String color = blankToNull(request.color());
		String notes = blankToNull(request.notes());
		boolean active = request.active() == null || request.active();

		var params = new MapSqlParameterSource().addValue("id", roomId).addValue("businessId", businessId)
				.addValue("locationId", locationId).addValue("code", code).addValue("name", name)
				.addValue("roomType", roomType).addValue("capacity", capacity).addValue("description", description)
				.addValue("color", color).addValue("notes", notes).addValue("active", active);

		try {
			namedParameterJdbcTemplate.update("""
					insert into agenda_room (id, business_id, location_id, code, name, room_type,
					    capacity, description, color, notes, active)
					values (:id, :businessId, :locationId, :code, :name, :roomType,
					    :capacity, :description, :color, :notes, :active)
					""", params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe una cabina con ese codigo en la sede.",
					Map.of("code", "El codigo ya esta registrado en esta sede."));
		}

		return findRoom(businessId, roomId);
	}

	public RoomResponse updateRoom(UUID businessId, UUID roomId, RoomRequest request) {
		List<UUID> locationIds = request.effectiveLocationIds();
		if (locationIds.isEmpty()) {
			throw new ConflictException("La sede es obligatoria.",
					Map.of("locationId", "Selecciona una sede para la cabina."));
		}
		UUID locationId = locationIds.getFirst();
		String code = request.code().trim().toLowerCase();
		String name = request.name().trim();
		String roomType = request.roomType().trim().toUpperCase();
		int capacity = request.capacity() != null && request.capacity() > 0 ? request.capacity() : 1;
		String description = blankToNull(request.description());
		String color = blankToNull(request.color());
		String notes = blankToNull(request.notes());
		boolean active = request.active() == null || request.active();

		var params = new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", roomId)
				.addValue("locationId", locationId).addValue("code", code).addValue("name", name)
				.addValue("roomType", roomType).addValue("capacity", capacity).addValue("description", description)
				.addValue("color", color).addValue("notes", notes).addValue("active", active);

		try {
			int updated = namedParameterJdbcTemplate.update("""
					update agenda_room
					set location_id = :locationId, code = :code, name = :name, room_type = :roomType,
					    capacity = :capacity, description = :description, color = :color,
					    notes = :notes, active = :active, updated_at = current_timestamp
					where business_id = :businessId
					  and id = :id
					""", params);
			if (updated == 0) {
				throw new ResourceNotFoundException("No se encontro la cabina o recurso solicitado.");
			}
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe una cabina con ese codigo en la sede.",
					Map.of("code", "El codigo ya esta registrado en esta sede."));
		}

		return findRoom(businessId, roomId);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private RowMapper<RoomResponse> roomRowMapper() {
		return (ResultSet rs, int rowNum) -> {
			UUID id = rs.getObject("id", UUID.class);
			UUID locationId = rs.getObject("location_id", UUID.class);
			String locationName = rs.getString("location_name");
			String code = rs.getString("code");
			String name = rs.getString("name");
			String roomType = rs.getString("room_type");
			int capacity = rs.getInt("capacity");
			String description = rs.getString("description");
			String color = rs.getString("color");
			String notes = rs.getString("notes");
			boolean active = rs.getBoolean("active");
			OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
			OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
			return new RoomResponse(id, locationId, locationName, code, name, roomType, capacity, description, color,
					notes, active, createdAt, updatedAt);
		};
	}
}
