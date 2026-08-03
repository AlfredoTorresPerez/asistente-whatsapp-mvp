package com.asistentewhatsapp.customers.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.asistentewhatsapp.customers.api.CustomerSearchResponse;

@Repository
public class CustomerSearchJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public CustomerSearchJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<CustomerSearchResponse> findByPhone(UUID businessId, String phoneDigits) {
		return jdbcTemplate.query("""
				select
				    id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    normalized_phone,
				    email,
				    created_at
				from customer
				where business_id = :businessId
				  and active = true
				  and (
				      normalized_phone = :phone
				       or regexp_replace(coalesce(normalized_phone, phone, ''), '[^\\d+]', '', 'g') = :phone
				       or regexp_replace(coalesce(phone, ''), '\\D', '', 'g') = :phoneDigits
				       or right(regexp_replace(coalesce(normalized_phone, phone, ''), '\\D', '', 'g'), 9) = :phoneDigits
				       or right(regexp_replace(coalesce(normalized_phone, phone, ''), '\\D', '', 'g'), 8) = :phoneDigits
				    )
				order by created_at desc
				limit 20
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("phone", phoneDigits)
						.addValue("phoneDigits", phoneDigits != null ? phoneDigits.replaceAll("[^0-9]", "") : ""),
				customerSearchRowMapper());
	}

	public List<CustomerSearchResponse> findByName(UUID businessId, String name) {
		String pattern = "%" + name.toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
		return jdbcTemplate.query("""
				select
				    id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    normalized_phone,
				    email,
				    created_at
				from customer
				where business_id = :businessId
				  and active = true
				  and (
				      lower(display_name) like :pattern
				       or lower(first_name) || ' ' || lower(last_name) like :pattern escape '\\'
				       or lower(first_name) like :pattern
				       or lower(last_name) like :pattern
				    )
				order by created_at desc
				limit 20
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("pattern", pattern),
				customerSearchRowMapper());
	}

	public List<CustomerSearchResponse> findByEmail(UUID businessId, String email) {
		String normalizedEmail = email.toLowerCase();
		return jdbcTemplate.query("""
				select
				    id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    normalized_phone,
				    email,
				    created_at
				from customer
				where business_id = :businessId
				  and active = true
				  and lower(email) = :email
				order by created_at desc
				limit 20
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("email", normalizedEmail),
				customerSearchRowMapper());
	}

	private RowMapper<CustomerSearchResponse> customerSearchRowMapper() {
		return (rs, rowNum) -> new CustomerSearchResponse(rs.getObject("id", UUID.class), rs.getString("first_name"),
				rs.getString("last_name"), rs.getString("display_name"), rs.getString("phone"),
				rs.getString("normalized_phone"), rs.getString("email"),
				rs.getObject("created_at", java.time.OffsetDateTime.class));
	}
}
