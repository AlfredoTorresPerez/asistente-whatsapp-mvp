package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.AssignmentGroupResponse;
import com.asistentewhatsapp.administration.api.AssignmentResponse;
import com.asistentewhatsapp.administration.api.AssignmentSummaryResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.time.LocalDate;
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
		validateProfessionalServiceAssignment(businessId, serviceId, professionalId);
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
		validateRoomServiceAssignment(businessId, serviceId, roomId);
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
			UUID serviceId, UUID locationId, String categoryCode, UUID professionalId, UUID roomId, String coverage) {
		var params = new MapSqlParameterSource().addValue("businessId", businessId);
		var where = new StringBuilder(" where s.business_id = :businessId ");

		if (serviceId != null) {
			where.append(" and s.id = :serviceId ");
			params.addValue("serviceId", serviceId);
		}

		if (locationId != null) {
			where.append("""
					and exists (
					    select 1
					    from aesthetic_service_location asl
					    where asl.business_id = s.business_id
					      and asl.service_id = s.id
					      and asl.location_id = :locationId
					      and asl.active = true
					)
					""");
			params.addValue("locationId", locationId);
		}

		if (categoryCode != null && !categoryCode.isBlank()) {
			where.append(" and c.code = :categoryCode ");
			params.addValue("categoryCode", categoryCode.trim());
		}

		if (professionalId != null) {
			where.append("""
					and exists (
					    select 1
					    from agenda_professional_service aps
					    where aps.business_id = s.business_id
					      and aps.service_id = s.id
					      and aps.professional_id = :professionalId
					)
					""");
			params.addValue("professionalId", professionalId);
		}

		if (roomId != null) {
			where.append("""
					and exists (
					    select 1
					    from agenda_room_service ars
					    where ars.business_id = s.business_id
					      and ars.service_id = s.id
					      and ars.room_id = :roomId
					)
					""");
			params.addValue("roomId", roomId);
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
				select s.id, s.code, s.name, c.code as category_code, c.name as category_name
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				%s
				order by s.name asc
				limit :limit
				offset :offset
				""".formatted(where), pageParams,
				(rs, rowNum) -> new ServiceRef(rs.getObject("id", UUID.class), rs.getString("code"),
						rs.getString("name"), rs.getString("category_code"), rs.getString("category_name")));

		List<AssignmentGroupResponse> groups = buildGroups(businessId, services, locationId);

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
		Long total = namedParameterJdbcTemplate.queryForObject("""
				select count(*)
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				""" + where, params.getValues(), Long.class);
		return total == null ? 0L : total;
	}

	private void validateProfessionalServiceAssignment(UUID businessId, UUID serviceId, UUID professionalId) {
		ServiceAssignmentRule service = findServiceAssignmentRule(businessId, serviceId);
		ProfessionalAssignmentRule professional = findProfessionalAssignmentRule(businessId, professionalId);

		if (!service.active()) {
			throw new ConflictException("El servicio no esta activo.",
					Map.of("serviceId", "Activa el servicio antes de asignar recursos."));
		}
		if (!professional.active()) {
			throw new ConflictException("El profesional no esta activo.",
					Map.of("professionalId", "Activa el profesional antes de asignarlo."));
		}
		if (service.requiredProfessionalLevel() != null && (professional.qualificationLevel() == null
				|| professional.qualificationLevel() < service.requiredProfessionalLevel())) {
			throw new ConflictException("El profesional no cumple el nivel requerido para el servicio.",
					Map.of("professionalId", "Selecciona un profesional habilitado para este servicio."));
		}
		if (service.requiresProfessionalCertification() && professional.certificationRef() == null) {
			throw new ConflictException("El servicio requiere certificacion profesional.",
					Map.of("professionalId", "Selecciona un profesional con certificacion registrada."));
		}
		if (professional.certificationValidUntil() != null
				&& professional.certificationValidUntil().isBefore(LocalDate.now())) {
			throw new ConflictException("La certificacion del profesional no esta vigente.",
					Map.of("professionalId", "Actualiza la certificacion o selecciona otro profesional."));
		}

		Integer compatibleLocations = namedParameterJdbcTemplate.queryForObject("""
				select count(*)
				from aesthetic_service_location asl
				join aesthetic_professional_location apl
				  on apl.business_id = asl.business_id
				 and apl.location_id = asl.location_id
				 and apl.professional_id = :professionalId
				 and apl.active = true
				join business_location bl
				  on bl.id = asl.location_id
				 and bl.business_id = asl.business_id
				 and bl.active = true
				where asl.business_id = :businessId
				  and asl.service_id = :serviceId
				  and asl.active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId)
				.addValue("professionalId", professionalId), Integer.class);
		if (compatibleLocations == null || compatibleLocations == 0) {
			throw new ConflictException("El profesional no atiende en una sede disponible para este servicio.",
					Map.of("professionalId", "Revisa las sedes autorizadas del profesional."));
		}
	}

	private void validateRoomServiceAssignment(UUID businessId, UUID serviceId, UUID roomId) {
		ServiceAssignmentRule service = findServiceAssignmentRule(businessId, serviceId);
		RoomAssignmentRule room = findRoomAssignmentRule(businessId, roomId);

		if (!service.active()) {
			throw new ConflictException("El servicio no esta activo.",
					Map.of("serviceId", "Activa el servicio antes de asignar recursos."));
		}
		if (!room.active()) {
			throw new ConflictException("La cabina no esta activa.",
					Map.of("roomId", "Activa la cabina antes de asignarla."));
		}
		if (!room.locationActive()) {
			throw new ConflictException("La sede de la cabina no esta activa.",
					Map.of("roomId", "Selecciona una cabina de una sede activa."));
		}
		if (!isRoomCompatible(service.categoryCode(), room.roomType())) {
			throw new ConflictException("La cabina no es compatible con la categoria del servicio.",
					Map.of("roomId", "Selecciona una cabina compatible."));
		}

		Integer compatibleLocations = namedParameterJdbcTemplate.queryForObject("""
				select count(*)
				from aesthetic_service_location asl
				where asl.business_id = :businessId
				  and asl.service_id = :serviceId
				  and asl.location_id = :locationId
				  and asl.active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId)
				.addValue("locationId", room.locationId()), Integer.class);
		if (compatibleLocations == null || compatibleLocations == 0) {
			throw new ConflictException("El servicio no esta disponible en la sede de la cabina.",
					Map.of("roomId", "Selecciona una cabina de una sede donde el servicio este disponible."));
		}
	}

	private ServiceAssignmentRule findServiceAssignmentRule(UUID businessId, UUID serviceId) {
		List<ServiceAssignmentRule> items = namedParameterJdbcTemplate.query("""
				select s.active,
				       c.code as category_code,
				       s.required_professional_level,
				       s.requires_professional_certification
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				where s.business_id = :businessId
				  and s.id = :serviceId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				(rs, rowNum) -> new ServiceAssignmentRule(rs.getBoolean("active"), rs.getString("category_code"),
						(Integer) rs.getObject("required_professional_level"),
						rs.getBoolean("requires_professional_certification")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el servicio solicitado.");
		}
		return items.getFirst();
	}

	private ProfessionalAssignmentRule findProfessionalAssignmentRule(UUID businessId, UUID professionalId) {
		List<ProfessionalAssignmentRule> items = namedParameterJdbcTemplate.query("""
				select active, qualification_level, certification_ref, certification_valid_until
				from aesthetic_professional
				where business_id = :businessId
				  and id = :professionalId
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId",
						professionalId),
				(rs, rowNum) -> new ProfessionalAssignmentRule(rs.getBoolean("active"),
						(Integer) rs.getObject("qualification_level"), rs.getString("certification_ref"),
						rs.getObject("certification_valid_until", LocalDate.class)));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el profesional solicitado.");
		}
		return items.getFirst();
	}

	private RoomAssignmentRule findRoomAssignmentRule(UUID businessId, UUID roomId) {
		List<RoomAssignmentRule> items = namedParameterJdbcTemplate.query("""
				select ar.active, ar.room_type, ar.location_id, bl.active as location_active
				from agenda_room ar
				join business_location bl
				  on bl.id = ar.location_id
				 and bl.business_id = ar.business_id
				where ar.business_id = :businessId
				  and ar.id = :roomId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("roomId", roomId),
				(rs, rowNum) -> new RoomAssignmentRule(rs.getBoolean("active"), rs.getString("room_type"),
						rs.getObject("location_id", UUID.class), rs.getBoolean("location_active")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la cabina solicitada.");
		}
		return items.getFirst();
	}

	private boolean isRoomCompatible(String categoryCode, String roomType) {
		String category = categoryCode == null ? "" : categoryCode.toUpperCase();
		String type = roomType == null ? "" : roomType.toUpperCase();
		if ("MULTIPROPOSITO".equals(type)) {
			return true;
		}
		return switch (category) {
			case "FACIAL", "PESTANAS_CEJAS", "MEDICINA_NO_INVASIVA" ->
				List.of("FACIAL", "FACIAL_CORPORAL", "CONSULTORIO").contains(type);
			case "CORPORAL", "MASAJES" -> List.of("CORPORAL", "FACIAL_CORPORAL").contains(type);
			case "DEPILACION" -> List.of("DEPILACION_MANOS", "FACIAL_CORPORAL", "CORPORAL").contains(type);
			case "MANICURE_PEDICURE" -> List.of("MANOS_PIES", "DEPILACION_MANOS").contains(type);
			case "PELUQUERIA" -> "PELUQUERIA".equals(type);
			case "MAQUILLAJE" -> List.of("MAQUILLAJE", "PELUQUERIA", "FACIAL").contains(type);
			default -> true;
		};
	}

	private List<String> findServiceLocationNames(UUID businessId, UUID serviceId, UUID locationId) {
		var params = new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId);
		String locationFilter = "";
		if (locationId != null) {
			params.addValue("locationId", locationId);
			locationFilter = " and bl.id = :locationId ";
		}
		return namedParameterJdbcTemplate.queryForList("""
				select bl.name
				from aesthetic_service_location asl
				join business_location bl
				  on bl.id = asl.location_id
				 and bl.business_id = asl.business_id
				where asl.business_id = :businessId
				  and asl.service_id = :serviceId
				  and asl.active = true
				%s
				order by bl.name
				""".formatted(locationFilter), params, String.class);
	}

	private List<AssignmentGroupResponse> buildGroups(UUID businessId, List<ServiceRef> services, UUID locationId) {
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
		String professionalLocationFilter = "";
		String roomLocationFilter = "";
		if (locationId != null) {
			params.addValue("locationId", locationId);
			professionalLocationFilter = """
					  and exists (
					      select 1
					      from aesthetic_professional_location apl
					      where apl.business_id = aps.business_id
					        and apl.professional_id = aps.professional_id
					        and apl.location_id = :locationId
					        and apl.active = true
					  )
					""";
			roomLocationFilter = " and ar.location_id = :locationId ";
		}
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
				%s
				order by ap.full_name asc
				""".formatted(professionalLocationFilter), params, assignmentRowMapper());
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
				%s
				order by ar.name asc
				""".formatted(roomLocationFilter), params, assignmentRowMapper());
		rooms.forEach(item -> roomsByService.get(item.serviceId()).add(item));

		List<AssignmentGroupResponse> groups = new ArrayList<>(services.size());
		for (ServiceRef service : services) {
			List<AssignmentResponse> professionalItems = professionalsByService.get(service.id());
			List<AssignmentResponse> roomItems = roomsByService.get(service.id());
			boolean covered = professionalItems.stream().anyMatch(AssignmentResponse::active)
					&& roomItems.stream().anyMatch(AssignmentResponse::active);
			groups.add(new AssignmentGroupResponse(service.id(), service.name(), service.code(), service.categoryCode(),
					service.categoryName(), findServiceLocationNames(businessId, service.id(), locationId),
					professionalItems, roomItems, professionalItems.size(), roomItems.size(), covered));
		}
		return groups;
	}

	private int totalPages(long totalItems, int size) {
		return size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
	}

	private record ServiceRef(UUID id, String code, String name, String categoryCode, String categoryName) {
	}

	private record ServiceAssignmentRule(boolean active, String categoryCode, Integer requiredProfessionalLevel,
			boolean requiresProfessionalCertification) {
	}

	private record ProfessionalAssignmentRule(boolean active, Integer qualificationLevel, String certificationRef,
			LocalDate certificationValidUntil) {
	}

	private record RoomAssignmentRule(boolean active, String roomType, UUID locationId, boolean locationActive) {
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
