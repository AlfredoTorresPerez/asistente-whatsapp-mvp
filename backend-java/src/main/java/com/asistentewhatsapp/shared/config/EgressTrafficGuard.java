package com.asistentewhatsapp.shared.config;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Interceptor de bloqueo de trafico saliente para el modo local seguro.
 *
 * <p>
 * Se registra en el {@code RestClient.Builder} compartido (unico punto de
 * salida HTTP del backend) y rechaza cualquier llamada a hosts que no sean
 * locales o del stack de contenedores. Cubre WhatsApp Cloud API, OpenAI,
 * Mercado Pago, Google Calendar y cualquier otra integracion HTTP que use el
 * builder compartido.
 *
 * <p>
 * En el perfil {@code local-meta-controlled} no se activa: la integracion real
 * con Meta requiere salida hacia {@code graph.facebook.com}.
 */
@Component
@Profile("local-safe")
@ConditionalOnProperty(prefix = "app.local-safe", name = "egress-guard-enabled", havingValue = "true", matchIfMissing = false)
public class EgressTrafficGuard implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgressTrafficGuard.class);

	private static final Set<String> ALLOWED_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]",
			"postgres", "mailpit", "tempo", "loki", "prometheus", "grafana", "alloy", "backend-java", "frontend-react");

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		URI uri = request.getURI();
		String host = uri.getHost();
		if (isAllowed(host)) {
			return execution.execute(request, body);
		}
		LOGGER.warn("EGRESS_BLOCKED host={} method={} uriPath={}", host == null ? "null" : host, request.getMethod(),
				maskPath(uri));
		throw new EgressTrafficBlockedException(host);
	}

	private boolean isAllowed(String host) {
		if (host == null || host.isBlank()) {
			return true;
		}
		String normalized = host.toLowerCase(Locale.ROOT);
		if (ALLOWED_HOSTS.contains(normalized)) {
			return true;
		}
		if (normalized.startsWith("[") && normalized.endsWith("]")) {
			String ipv6 = normalized.substring(1, normalized.length() - 1);
			return ALLOWED_HOSTS.contains(ipv6);
		}
		return normalized.endsWith(".local");
	}

	private String maskPath(URI uri) {
		String path = uri.getPath();
		return path == null || path.isBlank() ? "/" : path;
	}

	/**
	 * Lanzada cuando el guard bloquea una salida no autorizada.
	 */
	public static class EgressTrafficBlockedException extends RuntimeException {

		public EgressTrafficBlockedException(String host) {
			super("Trafico saliente bloqueado por la compuerta local-safe hacia host=" + host
					+ ". Solo se permiten hosts locales y del stack de contenedores.");
		}
	}
}
