package com.asistentewhatsapp.aiagents.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AiKnowledgeRepository {

	List<ServiceCatalogItem> findActiveServices(UUID businessId);

	Optional<ResponseRule> findActiveRule(UUID businessId, String code);

	List<EntityAlias> findActiveEntityAliases(UUID businessId);

	record ServiceCatalogItem(String code, String name, String categoryCode, Integer durationMinutes,
			BigDecimal priceBase) {
	}

	record ResponseRule(String code, String template, Map<String, Object> payload) {
	}

	record EntityAlias(String alias, String entityKey, String entityValue, Integer priority) {
	}
}
