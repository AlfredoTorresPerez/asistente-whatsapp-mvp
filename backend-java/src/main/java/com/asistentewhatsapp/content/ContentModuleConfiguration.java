package com.asistentewhatsapp.content;

import com.asistentewhatsapp.content.infrastructure.ContentItemJdbcRepository;
import com.asistentewhatsapp.content.infrastructure.ContentItemRowMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentModuleConfiguration {

    @Bean
    public ContentItemRowMapper contentItemRowMapper() {
        return new ContentItemRowMapper();
    }

    @Bean
    public ContentItemJdbcRepository contentItemJdbcRepository(
            org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbcTemplate,
            ContentItemRowMapper rowMapper) {
        return new ContentItemJdbcRepository(jdbcTemplate, rowMapper);
    }
}