package com.asistentewhatsapp.locations.infrastructure;

import com.asistentewhatsapp.locations.api.BusinessLocationResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessLocationJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public BusinessLocationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<BusinessLocationRecord> findAll(UUID businessId) {
		return jdbcTemplate.query("""
				select id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone,
				       latitude, longitude, daily_booking_capacity, active, created_at, updated_at
				from business_location
				where business_id = :businessId
				order by active desc, name asc
				""", new MapSqlParameterSource().addValue("businessId", businessId), recordRowMapper());
	}

	public List<BusinessLocationRecord> findActive(UUID businessId) {
		return jdbcTemplate.query("""
				select id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone,
				       latitude, longitude, daily_booking_capacity, active, created_at, updated_at
				from business_location
				where business_id = :businessId
				  and active = true
				order by name asc
				""", new MapSqlParameterSource().addValue("businessId", businessId), recordRowMapper());
	}

	public BusinessLocationRecord findById(UUID businessId, UUID locationId) {
		return findOptionalById(businessId, locationId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro la sede solicitada."));
	}

	public Optional<BusinessLocationRecord> findOptionalById(UUID businessId, UUID locationId) {
		if (locationId == null) {
			return Optional.empty();
		}
		List<BusinessLocationRecord> items = jdbcTemplate.query("""
				select id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone,
				       latitude, longitude, daily_booking_capacity, active, created_at, updated_at
				from business_location
				where business_id = :businessId
				  and id = :locationId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
				recordRowMapper());
		return items.stream().findFirst();
	}

	public BusinessLocationRecord findActiveById(UUID businessId, UUID locationId) {
		return findOptionalActiveById(businessId, locationId).orElseThrow(
				() -> new ResourceNotFoundException("No se encontro una sede activa para la operacion solicitada."));
	}

	public Optional<BusinessLocationRecord> findOptionalActiveById(UUID businessId, UUID locationId) {
		return findOptionalById(businessId, locationId).filter(BusinessLocationRecord::active);
	}

	public Optional<BusinessLocationRecord> findSingleActive(UUID businessId) {
		List<BusinessLocationRecord> items = findActive(businessId);
		return items.size() == 1 ? Optional.of(items.getFirst()) : Optional.empty();
	}

	public Optional<BusinessLocationRecord> findDefaultActive(UUID businessId) {
		List<BusinessLocationRecord> items = jdbcTemplate.query("""
				select id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone,
				       latitude, longitude, daily_booking_capacity, active, created_at, updated_at
				from business_location
				where business_id = :businessId
				  and active = true
				order by case when code = 'principal' then 0 else 1 end, created_at asc, name asc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId), recordRowMapper());
		return items.stream().findFirst();
	}

	public long countActive(UUID businessId) {
		Long count = jdbcTemplate.queryForObject("""
				select count(*)
				from business_location
				where business_id = :businessId
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId), Long.class);
		return count == null ? 0 : count;
	}

	public boolean existsByCode(UUID businessId, String code, UUID excludedLocationId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("code", code).addValue("excludedLocationId", excludedLocationId);
		Long count = jdbcTemplate.queryForObject("""
				select count(*)
				from business_location
				where business_id = :businessId
				  and code = :code
				  and (cast(:excludedLocationId as uuid) is null or id <> :excludedLocationId)
				""", parameters, Long.class);
		return count != null && count > 0;
	}

	public UUID insert(UUID businessId, String code, String name, String address, String city, String commune,
			String phone, String whatsappNumber, String timezone, boolean active) {
		UUID locationId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into business_location (
						    id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active
						) values (
						    :id, :businessId, :code, :name, :address, :city, :commune, :phone, :whatsappNumber, :timezone, :active
						)
						""",
				new MapSqlParameterSource().addValue("id", locationId).addValue("businessId", businessId)
						.addValue("code", code).addValue("name", name).addValue("address", address)
						.addValue("city", city).addValue("commune", commune).addValue("phone", phone)
						.addValue("whatsappNumber", whatsappNumber).addValue("timezone", timezone)
						.addValue("active", active));
		return locationId;
	}

	public void update(UUID businessId, UUID locationId, String code, String name, String address, String city,
			String commune, String phone, String whatsappNumber, String timezone, boolean active) {
		int updated = jdbcTemplate.update("""
				update business_location
				set code = :code,
				    name = :name,
				    address = :address,
				    city = :city,
				    commune = :commune,
				    phone = :phone,
				    whatsapp_number = :whatsappNumber,
				    timezone = :timezone,
				    active = :active,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :locationId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId)
				.addValue("code", code).addValue("name", name).addValue("address", address).addValue("city", city)
				.addValue("commune", commune).addValue("phone", phone).addValue("whatsappNumber", whatsappNumber)
				.addValue("timezone", timezone).addValue("active", active));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la sede solicitada.");
		}
	}

	public void deactivate(UUID businessId, UUID locationId) {
		int updated = jdbcTemplate.update("""
				update business_location
				set active = false,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :locationId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la sede solicitada.");
		}
	}

	public BusinessLocationResponse toResponse(BusinessLocationRecord record) {
		return new BusinessLocationResponse(record.id(), record.code(), record.name(), record.address(), record.city(),
				record.commune(), record.phone(), record.whatsappNumber(), record.timezone(), record.active(),
				record.createdAt(), record.updatedAt());
	}

	private RowMapper<BusinessLocationRecord> recordRowMapper() {
		return (resultSet, rowNum) -> new BusinessLocationRecord(resultSet.getObject("id", UUID.class),
				resultSet.getObject("business_id", UUID.class), resultSet.getString("code"),
				resultSet.getString("name"), resultSet.getString("address"), resultSet.getString("city"),
				resultSet.getString("commune"), resultSet.getString("phone"), resultSet.getString("whatsapp_number"),
				resultSet.getString("timezone"), resultSet.getBigDecimal("latitude"),
				resultSet.getBigDecimal("longitude"), (Integer) resultSet.getObject("daily_booking_capacity"),
				resultSet.getBoolean("active"), resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	public record BusinessLocationRecord(UUID id, UUID businessId, String code, String name, String address,
			String city, String commune, String phone, String whatsappNumber, String timezone, BigDecimal latitude,
			BigDecimal longitude, Integer dailyBookingCapacity, boolean active, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}
}
