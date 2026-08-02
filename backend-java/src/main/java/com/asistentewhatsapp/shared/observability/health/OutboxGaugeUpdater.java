package com.asistentewhatsapp.shared.observability.health;

import com.asistentewhatsapp.aiagents.application.AiReplyOutboxProcessor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxGaugeUpdater {

	private final AiReplyOutboxProcessor outboxProcessor;
	private final AtomicLong pendientesValue = new AtomicLong(0);
	private final AtomicLong antiguedadValue = new AtomicLong(0);

	public OutboxGaugeUpdater(AiReplyOutboxProcessor outboxProcessor, MeterRegistry meterRegistry) {
		this.outboxProcessor = outboxProcessor;
		Gauge.builder("assistente_outbox_pendientes", pendientesValue, AtomicLong::get)
				.description("Respuestas de IA pendientes de envio en el outbox").register(meterRegistry);
		Gauge.builder("assistente_outbox_antiguedad_maxima_segundos", antiguedadValue, AtomicLong::get)
				.description("Antiguedad maxima en segundos de la respuesta pendiente mas antigua del outbox")
				.register(meterRegistry);
	}

	@Scheduled(fixedRateString = "${app.observability.outbox-gauge-interval-ms:15000}")
	public void refresh() {
		AiReplyOutboxProcessor.AiOutboxStats stats = outboxProcessor.getStats();
		pendientesValue.set(stats.pending() + stats.processing());
		antiguedadValue.set(Math.max(stats.oldestAgeSeconds(), 0));
	}
}
