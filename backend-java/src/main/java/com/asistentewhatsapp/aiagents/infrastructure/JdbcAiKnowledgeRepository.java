package com.asistentewhatsapp.aiagents.infrastructure;

import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiKnowledgeRepository implements AiKnowledgeRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAiKnowledgeRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select s.code,
                               s.name,
                               c.code as category_code,
                               s.duration_minutes,
                               s.price_base
                        from aesthetic_service s
                        join aesthetic_service_category c
                          on c.id = s.category_id
                         and c.business_id = s.business_id
                        where s.business_id = :businessId
                          and s.active = true
                          and c.active = true
                        order by c.code asc, s.name asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new ServiceCatalogItem(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("category_code"),
                        rs.getInt("duration_minutes"),
                        rs.getBigDecimal("price_base")));
    }

    @Override
    public Optional<ResponseRule> findActiveRule(UUID businessId, String code) {
        List<ResponseRule> items = jdbcTemplate.query(
                """
                        select code,
                               coalesce(rule_payload ->> 'template', description) as template,
                               rule_payload::text as payload
                        from aesthetic_business_rule
                        where business_id = :businessId
                          and code = :code
                          and active = true
                        order by priority asc
                        limit 1
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("code", code),
                (rs, rowNum) -> new ResponseRule(
                        rs.getString("code"),
                        rs.getString("template"),
                        readPayload(rs.getString("payload"))));
        return items.stream().findFirst();
    }

    @Override
    public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select alias, entity_key, entity_value, priority
                        from ai_entity_alias
                        where business_id = :businessId
                          and active = true
                        order by priority desc, length(alias) desc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new EntityAlias(
                        rs.getString("alias"),
                        rs.getString("entity_key"),
                        rs.getString("entity_value"),
                        rs.getInt("priority")));
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
