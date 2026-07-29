package com.asistentewhatsapp.content.infrastructure;

import com.asistentewhatsapp.content.ContentItemType;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContentItemJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ContentItemRowMapper rowMapper;

	public ContentItemJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate, ContentItemRowMapper rowMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.rowMapper = rowMapper;
	}

	public List<ContentItemRecord> findAll(UUID businessId, ContentItemType type, String status, int page, int size) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("limit", size).addValue("offset", page * size);

		StringBuilder sql = new StringBuilder("""
				select id, business_id, type, image_path, text, status,
				       created_at, updated_at, created_by, updated_by, version
				from content_items
				where business_id = :businessId
				""");

		if (type != null) {
			sql.append(" and type = :type");
			params.addValue("type", type.name());
		}
		if (status != null && !status.isBlank()) {
			sql.append(" and status = :status");
			params.addValue("status", status);
		}

		sql.append(" order by created_at desc limit :limit offset :offset");

		return jdbcTemplate.query(sql.toString(), params, rowMapper);
	}

	public long count(UUID businessId, ContentItemType type, String status) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId);
		StringBuilder sql = new StringBuilder("select count(*) from content_items where business_id = :businessId");
		if (type != null) {
			sql.append(" and type = :type");
			params.addValue("type", type.name());
		}
		if (status != null && !status.isBlank()) {
			sql.append(" and status = :status");
			params.addValue("status", status);
		}
		Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
		return count != null ? count : 0;
	}

	public long countByTypeAndStatus(UUID businessId, ContentItemType type, String status) {
		return count(businessId, type, status);
	}

	public Optional<ContentItemRecord> findById(UUID businessId, UUID id) {
		List<ContentItemRecord> items = jdbcTemplate.query("""
				select id, business_id, type, image_path, text, status,
				       created_at, updated_at, created_by, updated_by, version
				from content_items
				where business_id = :businessId and id = :id
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id), rowMapper);
		return items.stream().findFirst();
	}

	public ContentItemRecord findByIdOrThrow(UUID businessId, UUID id) {
		return findById(businessId, id)
				.orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado: " + id));
	}

	public List<ContentItemRecord> findPublicActive(UUID businessId, ContentItemType type) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("businessId", businessId).addValue("status",
				"ACTIVE");

		StringBuilder sql = new StringBuilder("""
				select id, business_id, type, image_path, text, status,
				       created_at, updated_at, created_by, updated_by, version
				from content_items
				where business_id = :businessId and status = :status
				""");

		if (type != null) {
			sql.append(" and type = :type");
			params.addValue("type", type.name());
		}

		sql.append(" order by created_at desc");

		return jdbcTemplate.query(sql.toString(), params, rowMapper);
	}

	public UUID insert(UUID businessId, ContentItemType type, String imagePath, String text, String status,
			UUID createdBy) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into content_items (id, business_id, type, image_path, text, status, created_by, updated_by, version)
						values (:id, :businessId, :type, :imagePath, :text, :status, :createdBy, :updatedBy, 0)
						""",
				new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
						.addValue("type", type.name()).addValue("imagePath", imagePath).addValue("text", text.trim())
						.addValue("status", status).addValue("createdBy", createdBy).addValue("updatedBy", createdBy));
		return id;
	}

	public void update(UUID businessId, UUID id, ContentItemType type, String imagePath, String text, String status,
			UUID updatedBy, long version) {
		int updated = jdbcTemplate.update("""
				update content_items
				set type = :type,
				    image_path = :imagePath,
				    text = :text,
				    status = :status,
				    updated_by = :updatedBy,
				    updated_at = current_timestamp,
				    version = version + 1
				where business_id = :businessId and id = :id and version = :version
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id)
						.addValue("type", type.name()).addValue("imagePath", imagePath).addValue("text", text.trim())
						.addValue("status", status).addValue("updatedBy", updatedBy).addValue("version", version));
		if (updated == 0) {
			throw new OptimisticLockException(
					"El registro fue modificado por otro usuario. Recargue e intente nuevamente.");
		}
	}

	public void updateStatus(UUID businessId, UUID id, String status, UUID updatedBy, long version) {
		int updated = jdbcTemplate.update("""
				update content_items
				set status = :status,
				    updated_by = :updatedBy,
				    updated_at = current_timestamp,
				    version = version + 1
				where business_id = :businessId and id = :id and version = :version
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id)
				.addValue("status", status).addValue("updatedBy", updatedBy).addValue("version", version));
		if (updated == 0) {
			throw new OptimisticLockException(
					"El registro fue modificado por otro usuario. Recargue e intente nuevamente.");
		}
	}

	public void updateImagePath(UUID businessId, UUID id, String imagePath, UUID updatedBy, long version) {
		int updated = jdbcTemplate.update("""
				update content_items
				set image_path = :imagePath,
				    updated_by = :updatedBy,
				    updated_at = current_timestamp,
				    version = version + 1
				where business_id = :businessId and id = :id and version = :version
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id)
				.addValue("imagePath", imagePath).addValue("updatedBy", updatedBy).addValue("version", version));
		if (updated == 0) {
			throw new OptimisticLockException(
					"El registro fue modificado por otro usuario. Recargue e intente nuevamente.");
		}
	}

	public void clearImagePath(UUID businessId, UUID id, UUID updatedBy, long version) {
		int updated = jdbcTemplate.update("""
				update content_items
				set image_path = null,
				    updated_by = :updatedBy,
				    updated_at = current_timestamp,
				    version = version + 1
				where business_id = :businessId and id = :id and version = :version
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id)
				.addValue("updatedBy", updatedBy).addValue("version", version));
		if (updated == 0) {
			throw new OptimisticLockException(
					"El registro fue modificado por otro usuario. Recargue e intente nuevamente.");
		}
	}

	public void delete(UUID businessId, UUID id) {
		jdbcTemplate.update("""
				delete from content_items
				where business_id = :businessId and id = :id
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("id", id));
	}

	public static class OptimisticLockException extends RuntimeException {
		public OptimisticLockException(String message) {
			super(message);
		}
	}
}