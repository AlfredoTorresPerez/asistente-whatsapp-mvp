package com.asistentewhatsapp.content.infrastructure;

import com.asistentewhatsapp.content.ContentItemType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ContentItemRowMapper implements RowMapper<ContentItemRecord> {

    @Override
    public ContentItemRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ContentItemRecord(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("business_id")),
                ContentItemType.valueOf(rs.getString("type")),
                rs.getString("image_path"),
                rs.getString("text"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getObject("created_by") != null ? UUID.fromString(rs.getString("created_by")) : null,
                rs.getObject("updated_by") != null ? UUID.fromString(rs.getString("updated_by")) : null,
                rs.getLong("version")
        );
    }
}