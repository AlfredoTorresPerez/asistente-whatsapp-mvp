package com.asistentewhatsapp.aiagents.infrastructure;

import com.asistentewhatsapp.aiagents.application.AgentRoutingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiAgentJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiAgentJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void upsertConversationContext(AgentRoutingResult result) {
        jdbcTemplate.update(
                """
                        insert into ai_conversation_context (
                            id,
                            business_id,
                            conversation_id,
                            customer_id,
                            active_agent,
                            primary_intent,
                            secondary_intent,
                            urgency,
                            requires_human,
                            extracted_data,
                            missing_data,
                            summary,
                            created_at,
                            updated_at
                        ) values (
                            gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?,
                            cast(? as jsonb), cast(? as jsonb), ?, current_timestamp, current_timestamp
                        )
                        on conflict (business_id, conversation_id) do update set
                            customer_id = excluded.customer_id,
                            active_agent = excluded.active_agent,
                            primary_intent = excluded.primary_intent,
                            secondary_intent = excluded.secondary_intent,
                            urgency = excluded.urgency,
                            requires_human = excluded.requires_human,
                            extracted_data = excluded.extracted_data,
                            missing_data = excluded.missing_data,
                            summary = excluded.summary,
                            updated_at = current_timestamp
                        """,
                result.businessId(),
                result.conversationId(),
                result.customerId(),
                result.agentType().name(),
                result.primaryIntent().name(),
                result.secondaryIntent() == null ? null : result.secondaryIntent().name(),
                result.urgency(),
                result.requiresHuman(),
                toJson(result.extractedData()),
                toJson(result.missingData()),
                result.summaryForHuman());
    }

    public void insertDecisionLog(AgentRoutingResult result) {
        jdbcTemplate.update(
                """
                        insert into ai_agent_decision_log (
                            id,
                            business_id,
                            conversation_id,
                            customer_id,
                            primary_intent,
                            secondary_intent,
                            agent_type,
                            confidence,
                            urgency,
                            requires_human,
                            handoff_reason,
                            extracted_data,
                            missing_data,
                            response_to_customer,
                            created_at
                        ) values (
                            gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                            cast(? as jsonb), cast(? as jsonb), ?, current_timestamp
                        )
                        """,
                result.businessId(),
                result.conversationId(),
                result.customerId(),
                result.primaryIntent().name(),
                result.secondaryIntent() == null ? null : result.secondaryIntent().name(),
                result.agentType().name(),
                result.confidence(),
                result.urgency(),
                result.requiresHuman(),
                result.handoffReason(),
                toJson(result.extractedData()),
                toJson(result.missingData()),
                result.responseToCustomer());
    }

    public void insertHumanHandoff(AgentRoutingResult result) {
        jdbcTemplate.update(
                """
                        insert into human_handoff_request (
                            id,
                            business_id,
                            conversation_id,
                            customer_id,
                            urgency,
                            reason,
                            summary,
                            status,
                            created_at,
                            updated_at
                        ) values (
                            gen_random_uuid(), ?, ?, ?, ?, ?, ?, 'OPEN', current_timestamp, current_timestamp
                        )
                        """,
                result.businessId(),
                result.conversationId(),
                result.customerId(),
                result.urgency(),
                result.handoffReason(),
                result.summaryForHuman());
    }

    public void incrementMetric(AgentRoutingResult result) {
        jdbcTemplate.update(
                """
                        insert into ai_agent_metric_daily (
                            id,
                            business_id,
                            metric_date,
                            agent_type,
                            primary_intent,
                            total_messages,
                            total_handoffs,
                            created_at,
                            updated_at
                        ) values (
                            gen_random_uuid(), ?, ?, ?, ?, 1, ?, current_timestamp, current_timestamp
                        )
                        on conflict (business_id, metric_date, agent_type, primary_intent) do update set
                            total_messages = ai_agent_metric_daily.total_messages + 1,
                            total_handoffs = ai_agent_metric_daily.total_handoffs + excluded.total_handoffs,
                            updated_at = current_timestamp
                        """,
                result.businessId(),
                LocalDate.now(ZoneOffset.UTC),
                result.agentType().name(),
                result.primaryIntent().name(),
                result.requiresHuman() ? 1 : 0);
    }


    public Optional<ConversationContextSnapshot> findConversationContext(UUID businessId, UUID conversationId) {
        if (businessId == null || conversationId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                        select active_agent, primary_intent, secondary_intent, extracted_data, missing_data
                        from ai_conversation_context
                        where business_id = ? and conversation_id = ?
                        """,
                (rs, rowNum) -> new ConversationContextSnapshot(
                        parseAgentType(rs.getString("active_agent")),
                        parseAgentIntent(rs.getString("primary_intent")),
                        parseAgentIntent(rs.getString("secondary_intent")),
                        fromJsonMap(rs.getString("extracted_data")),
                        fromJsonList(rs.getString("missing_data"))),
                businessId,
                conversationId).stream().findFirst();
    }

    private AgentType parseAgentType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AgentType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AgentIntent parseAgentIntent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AgentIntent.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fromJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception exception) {
            return List.of();
        }
    }

    public record ConversationContextSnapshot(
            AgentType activeAgent,
            AgentIntent primaryIntent,
            AgentIntent secondaryIntent,
            Map<String, String> extractedData,
            List<String> missingData) {
    }


    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el contexto de agentes.", exception);
        }
    }
}
