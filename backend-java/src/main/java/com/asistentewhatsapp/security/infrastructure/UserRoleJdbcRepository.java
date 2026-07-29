package com.asistentewhatsapp.security.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRoleJdbcRepository {

	private static final String FIND_ROLES_SQL = """
			select r.code
			from user_role ur
			join role r on r.id = ur.role_id
			where ur.user_id = ?
			order by case r.code
			    when 'OWNER' then 1
			    when 'ADMIN' then 2
			    when 'SALES' then 3
			    when 'AGENT' then 4
			    else 99
			end,
			r.code
			""";

	private final JdbcTemplate jdbcTemplate;

	public UserRoleJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<String> findRoleCodesByUserId(UUID userId) {
		return jdbcTemplate.queryForList(FIND_ROLES_SQL, String.class, userId);
	}
}
