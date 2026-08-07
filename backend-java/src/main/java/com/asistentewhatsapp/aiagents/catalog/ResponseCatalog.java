package com.asistentewhatsapp.aiagents.catalog;

import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.ResponseDefinition;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Catálogo central de respuestas (Fase 9). Expone las plantillas por intención
 * y ranura definidas en el catálogo maestro. Las plantillas físicas de los
 * agentes (DB {@code aesthetic_business_rule} + builders) se resuelven en las
 * clases {@code *Agent}; este catálogo es el punto único de referencia de qué
 * ranura corresponde a cada intención y el respaldo determinista.
 */
@Component
public class ResponseCatalog {

	private static final String INTRO = "initial";
	private static final String MISSING_DATA = "missingData";
	private static final String SUCCESS = "success";
	private static final String HANDSOFF = "handoff";
	private static final String ERROR = "error";

	private final MasterConversationCatalog catalog;

	public ResponseCatalog(MasterConversationCatalog catalog) {
		this.catalog = catalog;
	}

	public static ResponseCatalog defaults() {
		return new ResponseCatalog(MasterConversationCatalog.shared());
	}

	public Optional<ResponseDefinition> findResponse(AgentIntent intent) {
		return intent == null ? Optional.empty() : catalog.findResponse(intent.name());
	}

	/** Código de plantilla de una ranura para una intención. */
	public Optional<String> templateCode(AgentIntent intent, String slot) {
		return findResponse(intent).map(ResponseDefinition::templates).map(templates -> templates.get(slot));
	}

	public Optional<String> introCode(AgentIntent intent) {
		return templateCode(intent, INTRO);
	}

	public Optional<String> missingDataCode(AgentIntent intent) {
		return templateCode(intent, MISSING_DATA);
	}

	public Optional<String> successCode(AgentIntent intent) {
		return templateCode(intent, SUCCESS);
	}

	public Optional<String> handoffCode(AgentIntent intent) {
		return templateCode(intent, HANDSOFF);
	}

	public Optional<String> errorCode(AgentIntent intent) {
		return templateCode(intent, ERROR);
	}
}