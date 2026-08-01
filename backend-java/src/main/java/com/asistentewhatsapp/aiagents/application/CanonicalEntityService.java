package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository;
import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository.CanonicalAliasRecord;
import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository.CanonicalEntityRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provee el catalogo de entidades canonicas (ai_canonical_entity) y sus aliases
 * (ai_entity_alias) con cache local por negocio y TTL configurable. Incluye la
 * resolucion por alias con scoring para matchear el texto del mensaje (o un
 * valor extraido) contra las entidades canonicas.
 */
@Service
public class CanonicalEntityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalEntityService.class);
	private static final double MIN_ALIAS_SCORE = 0.55;
	private static final Map<String, Double> ALIAS_TYPE_FACTOR = Map.of("PREFERRED", 1.0, "SYNONYM", 0.95,
			"REGIONALISM", 0.9, "ORTHOGRAPHIC_ERROR", 0.88, "CONTEXTUAL", 0.8);

	private final CanonicalEntityJdbcRepository repository;
	private final AiAgentProperties properties;
	private final ConcurrentHashMap<UUID, CacheEntry<CanonicalEntityRecord>> cacheByBusiness = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, CacheEntry<CanonicalAliasRecord>> aliasesCacheByBusiness = new ConcurrentHashMap<>();

	public CanonicalEntityService(CanonicalEntityJdbcRepository repository, AiAgentProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	public List<CanonicalEntityRecord> findActive(UUID businessId) {
		CacheEntry entry = cacheByBusiness.get(businessId);
		if (entry != null && !entry.expired(properties.getIntentCatalogCacheTtlSeconds())) {
			return entry.entities();
		}
		try {
			List<CanonicalEntityRecord> entities = repository.findActive(businessId);
			cacheByBusiness.put(businessId, new CacheEntry(List.copyOf(entities), Instant.now()));
			return entities;
		} catch (RuntimeException ex) {
			LOGGER.warn("CANONICAL_ENTITY_CACHE_FALLBACK businessId={} error={}", businessId, ex.getMessage());
			return entry != null ? entry.entities() : List.of();
		}
	}

	public List<CanonicalAliasRecord> findActiveAliases(UUID businessId) {
		CacheEntry entry = aliasesCacheByBusiness.get(businessId);
		if (entry != null && !entry.expired(properties.getIntentCatalogCacheTtlSeconds())) {
			return entry.entities();
		}
		try {
			List<CanonicalAliasRecord> aliases = repository.findActiveAliases(businessId);
			aliasesCacheByBusiness.put(businessId, new CacheEntry(List.copyOf(aliases), Instant.now()));
			return aliases;
		} catch (RuntimeException ex) {
			LOGGER.warn("CANONICAL_ALIAS_CACHE_FALLBACK businessId={} error={}", businessId, ex.getMessage());
			return entry != null ? entry.entities() : List.of();
		}
	}

	/**
	 * Resuelve los aliases presentes en el texto normalizado, ordenados por score
	 * descendente. El score combina confidence_base del alias, factor por
	 * alias_type, longitud y calidad del match.
	 */
	public List<AliasMatch> resolveAliases(UUID businessId, String normalizedText) {
		if (businessId == null || normalizedText == null || normalizedText.isBlank()) {
			return List.of();
		}
		List<AliasMatch> matches = new ArrayList<>();
		for (CanonicalAliasRecord alias : findActiveAliases(businessId)) {
			String normalizedAlias = normalize(alias.alias());
			if (normalizedAlias.isBlank() || !normalizedText.contains(normalizedAlias)) {
				continue;
			}
			double score = scoreMatch(alias, normalizedAlias, normalizedText, normalizedText.length());
			if (score >= MIN_ALIAS_SCORE) {
				matches.add(toMatch(alias, normalizedAlias, score));
			}
		}
		matches.sort(
				(first, second) -> Double.compare(second.confidence().doubleValue(), first.confidence().doubleValue()));
		return List.copyOf(matches);
	}

	/**
	 * Resuelve un valor extraido contra los aliases: el valor normalizado es igual
	 * al alias, lo contiene o es contenido por el alias.
	 */
	public Optional<AliasMatch> resolveValueByAlias(UUID businessId, String value) {
		if (businessId == null || value == null || value.isBlank()) {
			return Optional.empty();
		}
		String normalizedValue = normalize(value);
		AliasMatch best = null;
		double bestScore = MIN_ALIAS_SCORE;
		for (CanonicalAliasRecord alias : findActiveAliases(businessId)) {
			String normalizedAlias = normalize(alias.alias());
			if (normalizedAlias.isBlank()) {
				continue;
			}
			double matchQuality;
			if (normalizedValue.equals(normalizedAlias)) {
				matchQuality = 1.0;
			} else if (normalizedValue.contains(normalizedAlias)) {
				matchQuality = 0.95;
			} else if (normalizedAlias.contains(normalizedValue)) {
				matchQuality = 0.9;
			} else {
				continue;
			}
			double score = score(alias, normalizedAlias, matchQuality);
			if (score > bestScore) {
				bestScore = score;
				best = toMatch(alias, normalizedAlias, score);
			}
		}
		return Optional.ofNullable(best);
	}

	public void invalidate(UUID businessId) {
		cacheByBusiness.remove(businessId);
		aliasesCacheByBusiness.remove(businessId);
	}

	public void invalidateAll() {
		cacheByBusiness.clear();
		aliasesCacheByBusiness.clear();
	}

	private AliasMatch toMatch(CanonicalAliasRecord alias, String normalizedAlias, double score) {
		return new AliasMatch(alias.canonicalEntityId(), alias.entityType(), alias.canonicalName(),
				alias.displayName() == null || alias.displayName().isBlank()
						? alias.canonicalName()
						: alias.displayName(),
				alias.alias(), normalizedAlias, BigDecimal.valueOf(score), alias.referenceType(), alias.referenceId());
	}

	private double scoreMatch(CanonicalAliasRecord alias, String normalizedAlias, String normalizedText,
			int textLength) {
		int aliasTokens = normalizedAlias.split(" ").length;
		double matchQuality = 1.0;
		if (textLength > normalizedAlias.length()) {
			matchQuality = 0.95;
		}
		double wholeTokenBonus = wholeTokenSequence(normalizedText, normalizedAlias) ? 1.05 : 1.0;
		return score(alias, normalizedAlias, matchQuality) * wholeTokenBonus;
	}

	private double score(CanonicalAliasRecord alias, String normalizedAlias, double matchQuality) {
		double typeFactor = ALIAS_TYPE_FACTOR.getOrDefault(alias.aliasType() == null ? "SYNONYM" : alias.aliasType(),
				0.9);
		double lengthFactor = Math.min(1.0, normalizedAlias.split(" ").length / 3.0 + 0.4);
		double base = alias.confidenceBase() == null ? 0.85 : alias.confidenceBase().doubleValue();
		return Math.max(0.0, Math.min(1.0, base * typeFactor * lengthFactor * matchQuality));
	}

	private boolean wholeTokenSequence(String normalizedText, String normalizedAlias) {
		String[] tokens = normalizedAlias.split(" ");
		int start = 0;
		while (start <= normalizedText.length() - normalizedAlias.length()) {
			int idx = normalizedText.indexOf(normalizedAlias, start);
			if (idx < 0) {
				return false;
			}
			boolean leftBoundary = idx == 0 || normalizedText.charAt(idx - 1) == ' ';
			boolean rightBoundary = idx + normalizedAlias.length() == normalizedText.length()
					|| normalizedText.charAt(idx + normalizedAlias.length()) == ' ';
			if (leftBoundary && rightBoundary) {
				return true;
			}
			start = idx + normalizedAlias.length();
		}
		return false;
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value).trim().toLowerCase(Locale.ROOT);
	}

	public record AliasMatch(UUID canonicalEntityId, String entityType, String canonicalName, String displayName,
			String matchedAlias, String normalizedAlias, BigDecimal confidence, String referenceType,
			UUID referenceId) {
	}

	private record CacheEntry<T>(List<T> entities, Instant loadedAt) {

		boolean expired(long ttlSeconds) {
			return loadedAt.isBefore(Instant.now().minusSeconds(ttlSeconds));
		}
	}
}
