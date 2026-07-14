package com.asistentewhatsapp.security.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserPermissionJdbcRepository {

    private static final String FIND_PERMISSIONS_SQL = """
            select distinct p.code
            from user_role ur
            join role r on r.id = ur.role_id
            join role_permission rp on rp.role_id = r.id
            join permission p on p.id = rp.permission_id
            where ur.user_id = ?
              and r.active = true
              and p.active = true
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserPermissionJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findPermissionCodesByUserId(UUID userId) {
        return jdbcTemplate.queryForList(FIND_PERMISSIONS_SQL, String.class, userId);
    }
}