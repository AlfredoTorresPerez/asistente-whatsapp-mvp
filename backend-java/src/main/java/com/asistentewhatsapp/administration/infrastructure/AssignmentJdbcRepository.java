package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.AssignmentGroupResponse;
import com.asistentewhatsapp.administration.api.AssignmentResponse;
import com.asistentewhatsapp.administration.api.AssignmentSummaryResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

	public AssignmentResponse updateActive(UUID businessId, UUID assignmentId, boolean active) {
		int updated = namedParameterJdbcTemplate.update("""
				update agenda_professional_service
				set active = :active, updated_at = current_timestamp
				where id = :id and business_id = :businessId
				""", new MapSqlParameterSource().addValue("id", assignmentId).addValue("businessId", businessId)
				.addValue("active", active));

		if (updated == 0) {
			updated = namedParameterJdbcTemplate.update("""
					update agenda_room_service
					set active = :active, updated_at = current_timestamp
					where id = :id and business_id = :businessId
					""", new MapSqlParameterSource().addValue("id", assignmentId).addValue("businessId", businessId)
					.addValue("active", active));
		}

		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la asignacion solicitada.");
		}

		return findAssignment(businessId, assignmentId);
	}

	public PagedResponse<AssignmentGroupResponse> findGroups(UUID businessId, int page, int size, String search,
			UUID serviceId, String coverage) {
		var params = new MapSqlParameterSource().addValue("businessId", businessId);
		var where = new StringBuilder(" where s.business_id = :businessId ");

		if (serviceId != null) {
			where.append(" and s.id = :serviceId ");
			params.addValue("serviceId", serviceId);
		}

		if (search != null && !search.isBlank()) {
			params.addValue("search", "%" + search.trim().toLowerCase() + "%");
			where.append("""
					 and (s.name ilike :search
					      or s.code ilike :search
					      or exists (select 1 from agenda_professional_service aps
					                 join aesthetic_professional ap on ap.id = aps.professional_id
					                 where aps.business_id = s.business_id and aps.service_id = s.id
					                   and (ap.full_name ilike :search or ap.display_name ilike :search))
					      or exists (select 1 from agenda_room_service ars
					                 join agenda_room ar on ar.id = ars.room_id
					                 where ars.business_id = s.business_id and ars.service_id = s.id
					                   and (ar.name ilike :search or ar.code ilike :search)))
					""");
		}

		if ("covered".equalsIgnoreCase(coverage) || "partial".equalsIgnoreCase(coverage)
				|| "none".equalsIgnoreCase(coverage)) {
			String hasProfessional = """
					exists (select 1 from agenda_professional_service aps
					        where aps.business_id = s.business_id and aps.service_id = s.id and aps.active = true)
					""";
			String hasRoom = """
					exists (select 1 from agenda_room_service ars
					        where ars.business_id = s.business_id and ars.service_id = s.id and ars.active = true)
					""";
			switch (coverage.toLowerCase()) {
				case "covered" -> where.append(" and ").append(hasProfessional).append(" and ").append(hasRoom);
				case "partial" -> where.append(" and ((").append(hasProfessional).append(") or (").append(hasRoom)
						.append(")) and not ((").append(hasProfessional).append(") and (").append(hasRoom).append("))");
				case "none" -> where.append(" and not (").append(hasProfessional).append(") and not (").append(hasRoom)
						.append(")");
				default -> {
				}
			}
		}

		long totalItems = countServices(params, where.toString());
		int totalPages = totalPages(totalItems, size);

		var pageParams = new MapSqlParameterSource().addValues(params.getValues());
		pageParams.addValue("limit", size).addValue("offset", page * size);
		List<ServiceRef> services = namedParameterJdbcTemplate.query("""
				select s.id, s.code, s.name
				from aesthetic_service s
				%s
				order by s.name asc
				limit :limit
				offset :offset
				""".formatted(where), pageParams, (rs, rowNum) -> new ServiceRef(rs.getObject("id", UUID.class),
				rs.getString("code"), rs.getString("name")));

		List<AssignmentGroupResponse> groups = buildGroups(businessId, services);

		return new PagedResponse<>(groups, page, size, totalItems, totalPages);
	}

	public AssignmentSummaryResponse summary(UUID businessId) {
		List<AssignmentSummaryResponse> rows = jdbcTemplate.query(
				"""
						select
						    count(*) as total_services,
						    coalesce(sum(case when has_professional and has_room then 1 else 0 end), 0) as covered_services,
						    coalesce(sum(case when not has_professional and not has_room then 1 else 0 end), 0) as uncovered_services
						from (
						    select s.id,
						           exists (select 1 from agenda_professional_service aps
						                   where aps.business_id = s.business_id and aps.service_id = s.id and aps.active = true) as has_professional,
						           exists (select 1 from agenda_room_service ars
						                   where ars.business_id = s.business_id and ars.service_id = s.id and ars.active = true) as has_room
						    from aesthetic_service s
						    where s.business_id = ?
						) t
						""",
				(rs, rowNum) -> {
					long total = rs.getLong("total_services");
					long covered = rs.getLong("covered_services");
					long uncovered = rs.getLong("uncovered_services");
					return new AssignmentSummaryResponse(total, covered, total - covered - uncovered, uncovered);
				}, businessId);
		return rows.getFirst();
	}

	private long countServices(MapSqlParameterSource params, String where) {
		Long total = namedParameterJdbcTemplate.queryForObject("select count(*) from aesthetic_service s " + where,
				params.getValues(), Long.class);
		return total == null ? 0L : total;
	}

	private List<AssignmentGroupResponse> buildGroups(UUID businessId, List<ServiceRef> services) {
		if (services.isEmpty()) {
			return List.of();
		}

		List<UUID> serviceIds = services.stream().map(ServiceRef::id).toList();
		Map<UUID, List<AssignmentResponse>> professionalsByService = new LinkedHashMap<>();
		Map<UUID, List<AssignmentResponse>> roomsByService = new LinkedHashMap<>();
		serviceIds.forEach(id -> {
			professionalsByService.put(id, new ArrayList<>());
			roomsByService.put(id, new ArrayList<>());
		});

		var params = new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceIds", serviceIds);
		List<AssignmentResponse> professionals = namedParameterJdbcTemplate.query("""
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
				  and aps.service_id in (:serviceIds)
				order by ap.full_name asc
				""", params, assignmentRowMapper());
		professionals.forEach(item -> professionalsByService.get(item.serviceId()).add(item));

		List<AssignmentResponse> rooms = namedParameterJdbcTemplate.query("""
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
				  and ars.service_id in (:serviceIds)
				order by ar.name asc
				""", params, assignmentRowMapper());
		rooms.forEach(item -> roomsByService.get(item.serviceId()).add(item));

		List<AssignmentGroupResponse> groups = new ArrayList<>(services.size());
		for (ServiceRef service : services) {
			List<AssignmentResponse> professionalItems = professionalsByService.get(service.id());
			List<AssignmentResponse> roomItems = roomsByService.get(service.id());
			boolean covered = professionalItems.stream().anyMatch(AssignmentResponse::active)
					&& roomItems.stream().anyMatch(AssignmentResponse::active);
			groups.add(new AssignmentGroupResponse(service.id(), service.name(), service.code(), professionalItems,
					roomItems, professionalItems.size(), roomItems.size(), covered));
		}
		return groups;
	}

	private int totalPages(long totalItems, int size) {
		return size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
	}

	private record ServiceRef(UUID id, String code, String name) {
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
