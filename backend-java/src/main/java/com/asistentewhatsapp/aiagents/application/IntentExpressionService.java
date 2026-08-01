package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.IntentExpression;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provee las expresiones de intencion activas del catalogo normalizado
 * (ai_intent_expression) con cache local por negocio y TTL configurable. Si la
 * consulta falla, degrada a la ultima vista en cache o a vacio: el fallback de
 * deteccion de intenciones es siempre el detector Java.
 */
@Service
public class IntentExpressionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IntentExpressionService.class);

	private final AiKnowledgeRepository repository;
	private final AiAgentProperties properties;
	private final ConcurrentHashMap<UUID, CacheEntry> cacheByBusiness = new ConcurrentHashMap<>();

	public IntentExpressionService(AiKnowledgeRepository repository, AiAgentProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	public List<IntentExpression> findActive(UUID businessId) {
		CacheEntry entry = cacheByBusiness.get(businessId);
		if (entry != null && !entry.expired(properties.getIntentCatalogCacheTtlSeconds())) {
			return entry.expressions();
		}
		try {
			List<IntentExpression> expressions = repository.findActiveIntentExpressions(businessId);
			cacheByBusiness.put(businessId, new CacheEntry(List.copyOf(expressions), Instant.now()));
			return expressions;
		} catch (RuntimeException ex) {
			LOGGER.warn("INTENT_EXPRESSION_CACHE_FALLBACK businessId={} error={}", businessId, ex.getMessage());
			return entry != null ? entry.expressions() : List.of();
		}
	}

	public void invalidate(UUID businessId) {
		cacheByBusiness.remove(businessId);
	}

	public void invalidateAll() {
		cacheByBusiness.clear();
	}

	private record CacheEntry(List<IntentExpression> expressions, Instant loadedAt) {

		boolean expired(long ttlSeconds) {
			return loadedAt.isBefore(Instant.now().minusSeconds(ttlSeconds));
		}
	}
}
