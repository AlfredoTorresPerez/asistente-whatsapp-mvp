package com.asistentewhatsapp.shared.config;

import com.asistentewhatsapp.shared.observability.CorrelationIdFilter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	private final Tracer tracer;

	public RestClientConfig(Tracer tracer) {
		this.tracer = tracer;
	}

	@Bean
	RestClient.Builder restClientBuilder() {
		return RestClient.builder()
				.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
						.withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(10))))
				.requestInterceptor(correlationPropagationInterceptor());
	}

	private ClientHttpRequestInterceptor correlationPropagationInterceptor() {
		return (request, body, execution) -> {
			HttpHeaders headers = request.getHeaders();
			if (!headers.containsKey("X-Correlation-Id")) {
				headers.set("X-Correlation-Id", CorrelationIdFilter.currentCorrelationId());
			}
			Span currentSpan = tracer.currentSpan();
			if (currentSpan != null) {
				String traceId = currentSpan.context().traceId();
				String spanId = currentSpan.context().spanId();
				if (traceId != null && spanId != null && !headers.containsKey("traceparent")) {
					headers.set("traceparent", "00-" + traceId + "-" + spanId + "-01");
				}
			}
			return execution.execute(request, body);
		};
	}
}
