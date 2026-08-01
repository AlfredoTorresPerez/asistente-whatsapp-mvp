package com.asistentewhatsapp.administration.infrastructure;

import com.asistentewhatsapp.administration.api.AdminSummaryResponse;
import com.asistentewhatsapp.administration.api.AdminRoleResponse;
import com.asistentewhatsapp.administration.api.AdminUserRequest;
import com.asistentewhatsapp.administration.api.AdminUserResponse;
import com.asistentewhatsapp.administration.api.SecurityPolicyRequest;
import com.asistentewhatsapp.administration.api.SecurityPolicyResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class AdministrationJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AdministrationJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public AdminSummaryResponse findSummary(UUID businessId) {
		CompanySummaryProjection companyProjection = jdbcTemplate.queryForObject("""
				select id, company_name
				from business
				where id = ?
				""", new CompanySummaryRowMapper(), businessId);

		Map<String, Object> usersProjection = jdbcTemplate.queryForMap("""
				select count(*) as total,
				       count(*) filter (where status = 'ACTIVE') as active
				from user_account
				where business_id = ?
				""", businessId);

		String whatsAppWebStatus = jdbcTemplate.queryForObject("""
				select coalesce(
				    (
				        select status
				        from channel_account
				        where business_id = ?
				          and channel_type = 'WHATSAPP'
				          and provider_name in ('META_CLOUD_API', 'SIMULATED')
				        order by created_at desc
				        limit 1
				    ),
				    'DISCONNECTED'
				)
				""", String.class, businessId);

		Integer sessionTimeoutMinutes = jdbcTemplate.queryForObject("""
				select session_timeout_minutes
				from security_policy
				where business_id = ?
				""", Integer.class, businessId);

		if (companyProjection == null) {
			throw new IllegalStateException("No se encontro la empresa para el resumen administrativo.");
		}

		return new AdminSummaryResponse(
				new AdminSummaryResponse.CompanySummary(companyProjection.id(), companyProjection.companyName()),
				new AdminSummaryResponse.UsersSummary(((Number) usersProjection.get("total")).longValue(),
						((Number) usersProjection.get("active")).longValue()),
				new AdminSummaryResponse.WhatsAppChannelSummary(
						whatsAppWebStatus == null ? "DISCONNECTED" : whatsAppWebStatus),
				new AdminSummaryResponse.SecuritySummary(sessionTimeoutMinutes == null ? 30 : sessionTimeoutMinutes));
	}

	public PagedResponse<AdminUserResponse> findAdminUsers(UUID businessId, int page, int size, String search,
			String role, String status) {
		QueryParts queryParts = buildAdminUserListQuery(businessId, search, role, status);
		Long totalItems = namedParameterJdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotalItems == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);

		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<AdminUserResponse> items = namedParameterJdbcTemplate.query("""
				select
				    ua.id,
				    ua.first_name,
				    ua.last_name,
				    ua.email,
				    ua.phone,
				    coalesce(r.code, 'AGENT') as role,
				    ua.status,
				    ua.timezone,
				    ua.last_login_at,
				    ua.failed_login_attempts,
				    ua.created_at,
				    ua.updated_at
				""" + queryParts.fromAndWhere() + """
				order by ua.created_at desc, ua.email asc
				limit :limit
				offset :offset
				""", parameters, adminUserRowMapper());

		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
	}

	public AdminUserResponse findAdminUser(UUID businessId, UUID userId) {
		List<AdminUserResponse> items = namedParameterJdbcTemplate.query("""
				select
				    ua.id,
				    ua.first_name,
				    ua.last_name,
				    ua.email,
				    ua.phone,
				    coalesce(r.code, 'AGENT') as role,
				    ua.status,
				    ua.timezone,
				    ua.last_login_at,
				    ua.failed_login_attempts,
				    ua.created_at,
				    ua.updated_at
				from user_account ua
				left join user_role ur on ur.user_id = ua.id and ur.business_id = ua.business_id
				left join role r on r.id = ur.role_id
				where ua.business_id = :businessId
				  and ua.id = :userId
				order by r.code asc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("userId", userId),
				adminUserRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el usuario solicitado.");
		}
		return items.getFirst();
	}

	public List<AdminRoleResponse> findAdminRoles() {
		return namedParameterJdbcTemplate.query("""
				select
				    r.id,
				    r.code,
				    r.name,
				    r.description,
				    count(rp.permission_id) as permission_count
				from role r
				left join role_permission rp on rp.role_id = r.id
				where r.active = true
				group by r.id, r.code, r.name, r.description
				order by
				    case r.code
				        when 'OWNER' then 1
				        when 'ADMIN' then 2
				        when 'AGENT' then 3
				        when 'SALES' then 4
				        else 99
				    end,
				    r.name
				""", new MapSqlParameterSource(),
				(resultSet, rowNum) -> new AdminRoleResponse(resultSet.getObject("id", UUID.class),
						resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("description"),
						resultSet.getLong("permission_count")));
	}

	public AdminUserResponse insertAdminUser(UUID businessId, AdminUserRequest request, String role, String status,
			String timezone, String passwordHash) {
		UUID userId = UUID.randomUUID();
		UUID userRoleId = UUID.randomUUID();
		UUID roleId = findRoleId(role);
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("id", userId)
				.addValue("businessId", businessId).addValue("firstName", request.firstName().trim())
				.addValue("lastName", request.lastName().trim()).addValue("email", request.email().trim().toLowerCase())
				.addValue("phone", blankToNull(request.phone())).addValue("passwordHash", passwordHash)
				.addValue("timezone", timezone).addValue("status", status).addValue("userRoleId", userRoleId)
				.addValue("roleId", roleId);

		try {
			namedParameterJdbcTemplate.update("""
					insert into user_account (
					    id,
					    business_id,
					    first_name,
					    last_name,
					    email,
					    phone,
					    password_hash,
					    timezone,
					    status
					) values (
					    :id,
					    :businessId,
					    :firstName,
					    :lastName,
					    :email,
					    :phone,
					    :passwordHash,
					    :timezone,
					    :status
					)
					""", parameters);
			namedParameterJdbcTemplate.update("""
					insert into user_role (
					    id,
					    business_id,
					    user_id,
					    role_id
					) values (
					    :userRoleId,
					    :businessId,
					    :id,
					    :roleId
					)
					""", parameters);
		} catch (DuplicateKeyException exception) {
			throw new ConflictException("Ya existe un usuario con ese correo en la empresa.",
					Map.of("email", "El correo ya esta registrado."));
		}

		return findAdminUser(businessId, userId);
	}

	public AdminUserResponse updateAdminUser(UUID businessId, UUID userId, AdminUserRequest request, String role,
			String status, String timezone) {
		UUID roleId = findRoleId(role);
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("userId", userId).addValue("firstName", request.firstName().trim())
				.addValue("lastName", request.lastName().trim()).addValue("email", request.email().trim().toLowerCase())
				.addValue("phone", blankToNull(request.phone())).addValue("timezone", timezone)
				.addValue("status", status).addValue("roleId", roleId);

		try {
			int updated = namedParameterJdbcTemplate.update("""
					update user_account
					set first_name = :firstName,
					    last_name = :lastName,
					    email = :email,
					    phone = :phone,
					    timezone = :timezone,
					    status = :status,
					    updated_at = current_timestamp
					where business_id = :businessId
					  and id = :userId
					""", parameters);
			if (updated == 0) {
				throw new ResourceNotFoundException("No se encontro el usuario solicitado.");
			}
		} catch (DuplicateKeyException exception) {
			throw new ConflictException("Ya existe un usuario con ese correo en la empresa.",
					Map.of("email", "El correo ya esta registrado."));
		}

		namedParameterJdbcTemplate.update("""
				delete from user_role
				where business_id = :businessId
				  and user_id = :userId
				""", parameters);
		namedParameterJdbcTemplate.update("""
				insert into user_role (
				    id,
				    business_id,
				    user_id,
				    role_id
				) values (
				    :userRoleId,
				    :businessId,
				    :userId,
				    :roleId
				)
				""", parameters.addValue("userRoleId", UUID.randomUUID()));

		return findAdminUser(businessId, userId);
	}

	public SecurityPolicyResponse findSecurityPolicy(UUID businessId) {
		List<SecurityPolicyResponse> items = namedParameterJdbcTemplate.query("""
				select
				    sp.id,
				    sp.session_timeout_minutes,
				    sp.password_min_length,
				    sp.require_uppercase,
				    sp.require_number,
				    sp.require_symbol,
				    sp.max_failed_login_attempts,
				    sp.updated_at,
				    (
				        select count(*)
				        from user_account ua
				        where ua.business_id = sp.business_id
				          and ua.status = 'ACTIVE'
				    ) as active_users,
				    (
				        select count(*)
				        from user_account ua
				        where ua.business_id = sp.business_id
				          and ua.status = 'LOCKED'
				    ) as locked_users,
				    (
				        select count(*)
				        from audit_log al
				        where al.business_id = sp.business_id
				          and al.occurred_at >= current_timestamp - interval '7 days'
				    ) as audit_events_last_7_days
				from security_policy sp
				where sp.business_id = :businessId
				""", new MapSqlParameterSource().addValue("businessId", businessId), securityPolicyRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la politica de seguridad de la empresa.");
		}
		return items.getFirst();
	}

	public SecurityPolicyResponse updateSecurityPolicy(UUID businessId, SecurityPolicyRequest request) {
		int updated = namedParameterJdbcTemplate.update("""
				update security_policy
				set session_timeout_minutes = :sessionTimeoutMinutes,
				    password_min_length = :passwordMinLength,
				    require_uppercase = :requireUppercase,
				    require_number = :requireNumber,
				    require_symbol = :requireSymbol,
				    max_failed_login_attempts = :maxFailedLoginAttempts,
				    updated_at = current_timestamp
				where business_id = :businessId
				""", new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("sessionTimeoutMinutes", request.sessionTimeoutMinutes())
				.addValue("passwordMinLength", request.passwordMinLength())
				.addValue("requireUppercase", request.requireUppercase())
				.addValue("requireNumber", request.requireNumber()).addValue("requireSymbol", request.requireSymbol())
				.addValue("maxFailedLoginAttempts", request.maxFailedLoginAttempts()));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la politica de seguridad de la empresa.");
		}
		return findSecurityPolicy(businessId);
	}

	private QueryParts buildAdminUserListQuery(UUID businessId, String search, String role, String status) {
		StringBuilder sql = new StringBuilder("""
				from user_account ua
				left join user_role ur on ur.user_id = ua.id and ur.business_id = ua.business_id
				left join role r on r.id = ur.role_id
				where ua.business_id = :businessId
				""");
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);

		if (search != null) {
			sql.append("""
					 and (
					    ua.first_name ilike :search
					    or ua.last_name ilike :search
					    or ua.email ilike :search
					    or coalesce(ua.phone, '') ilike :search
					 )
					""");
			parameters.addValue("search", "%" + search + "%");
		}

		if (role != null) {
			sql.append(" and r.code = :role ");
			parameters.addValue("role", role);
		}

		if (status != null) {
			sql.append(" and ua.status = :status ");
			parameters.addValue("status", status);
		}

		return new QueryParts(sql.toString(), parameters);
	}

	private UUID findRoleId(String role) {
		List<UUID> items = namedParameterJdbcTemplate.query("""
				select id
				from role
				where code = :role
				  and active = true
				""", new MapSqlParameterSource().addValue("role", role),
				(resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el rol solicitado.");
		}
		return items.getFirst();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private RowMapper<AdminUserResponse> adminUserRowMapper() {
		return (resultSet, rowNum) -> new AdminUserResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("first_name"), resultSet.getString("last_name"), resultSet.getString("email"),
				resultSet.getString("phone"), resultSet.getString("role"), resultSet.getString("status"),
				resultSet.getString("timezone"), resultSet.getObject("last_login_at", OffsetDateTime.class),
				resultSet.getInt("failed_login_attempts"), resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<SecurityPolicyResponse> securityPolicyRowMapper() {
		return (resultSet, rowNum) -> new SecurityPolicyResponse(resultSet.getObject("id", UUID.class),
				resultSet.getInt("session_timeout_minutes"), resultSet.getInt("password_min_length"),
				resultSet.getBoolean("require_uppercase"), resultSet.getBoolean("require_number"),
				resultSet.getBoolean("require_symbol"), resultSet.getInt("max_failed_login_attempts"),
				resultSet.getLong("active_users"), resultSet.getLong("locked_users"),
				resultSet.getLong("audit_events_last_7_days"), resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private record CompanySummaryProjection(UUID id, String companyName) {
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}

	private static class CompanySummaryRowMapper implements RowMapper<CompanySummaryProjection> {

		@Override
		public CompanySummaryProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new CompanySummaryProjection(resultSet.getObject("id", UUID.class),
					resultSet.getString("company_name"));
		}
	}
}
