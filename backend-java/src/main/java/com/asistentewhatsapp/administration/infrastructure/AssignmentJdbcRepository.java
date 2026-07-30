package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.AssignmentResponse;
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
public class AssignmentJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AssignmentJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public List<AssignmentResponse> findAssignments(UUID businessId, UUID serviceId, UUID professionalId, UUID roomId) {
		var sql = new StringBuilder("""
				select
				    aps.id, aps.business_id, aps.professional_id, null as room_id,
				    aps.service_id, aps.active, aps.created_at, aps.updated_at,
				    s.name as service_name, s.code as service_code,
				    ap.full_name as professional_name, ap.display_name as professional_display,
				    null as room_name, null as room_code,
				    'PROFESSIONAL_SERVICE' as assignment_type
				from agenda_professional_service aps
				join aesthetic_service s on s.id = aps.service_id
				join aesthetic_professional ap on ap.id = aps.professional_id
				where aps.business_id = :businessId
				""");
		var params = new MapSqlParameterSource().addValue("businessId", businessId);

		if (serviceId != null) {
			sql.append(" and aps.service_id = :serviceId ");
			params.addValue("serviceId", serviceId);
		}
		if (professionalId != null) {
			sql.append(" and aps.professional_id = :professionalId ");
			params.addValue("professionalId", professionalId);
		}

		sql.append("""
				union all
				select
				    ars.id, ars.business_id, null as professional_id, ars.room_id,
				    ars.service_id, ars.active, ars.created_at, ars.updated_at,
				    s.name as service_name, s.code as service_code,
				    null as professional_name, null as professional_display,
				    ar.name as room_name, ar.code as room_code,
				    'ROOM_SERVICE' as assignment_type
				from agenda_room_service ars
				join aesthetic_service s on s.id = ars.service_id
				join agenda_room ar on ar.id = ars.room_id
				where ars.business_id = :businessId
				""");

		if (roomId != null) {
			sql.append(" and ars.room_id = :roomId ");
			params.addValue("roomId", roomId);
		}

		sql.append(" order by service_name, assignment_type, professional_name nulls last, room_name nulls last");

		return namedParameterJdbcTemplate.query(sql.toString(), params, assignmentRowMapper());
	}

	public AssignmentResponse insertProfessionalService(UUID businessId, UUID serviceId, UUID professionalId) {
		if (professionalId == null) {
			throw new ConflictException("El profesional es obligatorio.",
					Map.of("professionalId", "Selecciona un profesional."));
		}
		UUID id = UUID.randomUUID();
		var params = new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
				.addValue("professionalId", professionalId).addValue("serviceId", serviceId);

		try {
			namedParameterJdbcTemplate.update("""
					insert into agenda_professional_service (id, business_id, professional_id, service_id, active)
					values (:id, :businessId, :professionalId, :serviceId, true)
					""", params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("El profesional ya esta asignado a este servicio.",
					Map.of("assignment", "La asignacion ya existe."));
		}

		return findAssignment(businessId, id);
	}

	public AssignmentResponse insertRoomService(UUID businessId, UUID serviceId, UUID roomId) {
		if (roomId == null) {
			throw new ConflictException("La cabina es obligatoria.", Map.of("roomId", "Selecciona una cabina."));
		}
		UUID id = UUID.randomUUID();
		var params = new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
				.addValue("roomId", roomId).addValue("serviceId", serviceId);

		try {
			namedParameterJdbcTemplate.update("""
					insert into agenda_room_service (id, business_id, room_id, service_id, active)
					values (:id, :businessId, :roomId, :serviceId, true)
					""", params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("La cabina ya esta asignada a este servicio.",
					Map.of("assignment", "La asignacion ya existe."));
		}

		return findAssignment(businessId, id);
	}

	public void deleteAssignment(UUID businessId, UUID assignmentId) {
		int deleted = 0;

		deleted += namedParameterJdbcTemplate.update("""
				delete from agenda_professional_service
				where id = :id and business_id = :businessId
				""", new MapSqlParameterSource().addValue("id", assignmentId).addValue("businessId", businessId));

		deleted += namedParameterJdbcTemplate.update("""
				delete from agenda_room_service
				where id = :id and business_id = :businessId
				""", new MapSqlParameterSource().addValue("id", assignmentId).addValue("businessId", businessId));

		if (deleted == 0) {
			throw new ResourceNotFoundException("No se encontro la asignacion solicitada.");
		}
	}

	private AssignmentResponse findAssignment(UUID businessId, UUID assignmentId) {
		List<AssignmentResponse> items = namedParameterJdbcTemplate.query("""
				select
				    aps.id, aps.business_id, aps.professional_id, null as room_id,
				    aps.service_id, aps.active, aps.created_at, aps.updated_at,
				    s.name as service_name, s.code as service_code,
				    ap.full_name as professional_name, ap.display_name as professional_display,
				    null as room_name, null as room_code,
				    'PROFESSIONAL_SERVICE' as assignment_type
				from agenda_professional_service aps
				join aesthetic_service s on s.id = aps.service_id
				join aesthetic_professional ap on ap.id = aps.professional_id
				where aps.business_id = :businessId and aps.id = :assignmentId
				union all
				select
				    ars.id, ars.business_id, null as professional_id, ars.room_id,
				    ars.service_id, ars.active, ars.created_at, ars.updated_at,
				    s.name as service_name, s.code as service_code,
				    null as professional_name, null as professional_display,
				    ar.name as room_name, ar.code as room_code,
				    'ROOM_SERVICE' as assignment_type
				from agenda_room_service ars
				join aesthetic_service s on s.id = ars.service_id
				join agenda_room ar on ar.id = ars.room_id
				where ars.business_id = :businessId and ars.id = :assignmentId
				limit 1
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("assignmentId", assignmentId),
				assignmentRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la asignacion solicitada.");
		}
		return items.getFirst();
	}

	private RowMapper<AssignmentResponse> assignmentRowMapper() {
		return (ResultSet rs, int rowNum) -> {
			UUID id = rs.getObject("id", UUID.class);
			UUID serviceId = rs.getObject("service_id", UUID.class);
			String serviceName = rs.getString("service_name");
			String serviceCode = rs.getString("service_code");
			UUID professionalId = rs.getObject("professional_id", UUID.class);
			String professionalName = rs.getString("professional_name");
			if (professionalName == null) {
				professionalName = rs.getString("professional_display");
			}
			UUID roomId = rs.getObject("room_id", UUID.class);
			String roomName = rs.getString("room_name");
			String roomCode = rs.getString("room_code");
			String assignmentType = rs.getString("assignment_type");
			boolean active = rs.getBoolean("active");
			OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
			OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
			return new AssignmentResponse(id, serviceId, serviceName, serviceCode, professionalId, professionalName,
					roomId, roomName, roomCode, assignmentType, active, createdAt, updatedAt);
		};
	}
}
