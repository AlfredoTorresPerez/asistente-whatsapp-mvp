package com.asistentewhatsapp.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BusinessMetricsTest {

	private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@Test
	void registersAllContractCounters() {
		BusinessMetrics metrics = new BusinessMetrics(meterRegistry);

		metrics.incrementWhatsappMensajesRecibidos();
		metrics.incrementWhatsappMensajesEnviados();
		metrics.incrementWhatsappMensajesFallidos();
		metrics.incrementWhatsappWebhooksRecibidos();
		metrics.incrementWhatsappWebhooksFirmaInvalida();
		metrics.incrementWhatsappEventosDuplicados();
		metrics.incrementConversacionesIniciadas();
		metrics.incrementConversacionesDerivadas();
		metrics.incrementIntencionesAmbiguas();
		metrics.incrementReservasCreadas();
		metrics.incrementReservasConfirmadas();
		metrics.incrementReservasReprogramadas();
		metrics.incrementReservasCanceladas();
		metrics.incrementReservasConflicto();
		metrics.incrementReservasExpiradas();
		metrics.incrementDisponibilidadConsultas();
		metrics.incrementDisponibilidadSinHorarios();
		metrics.incrementIaSolicitudes();
		metrics.incrementIaRespuestasExitosas();
		metrics.incrementIaRespuestasFallidas();
		metrics.incrementIaModoSeguro();
		metrics.incrementNotificacionesEnviadas();
		metrics.incrementNotificacionesFallidas();
		metrics.incrementNotificacionesReintentos();
		metrics.incrementOutboxProcesadas();
		metrics.incrementOutboxFallidas();

		assertThat(counterValue("assistente_whatsapp_mensajes_recibidos_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_whatsapp_mensajes_enviados_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_whatsapp_mensajes_fallidos_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_whatsapp_webhooks_recibidos_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_whatsapp_webhooks_firma_invalida_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_whatsapp_eventos_duplicados_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_conversaciones_iniciadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_conversaciones_derivadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_intenciones_ambiguas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_creadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_confirmadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_reprogramadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_canceladas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_conflicto_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_reservas_expiradas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_disponibilidad_consultas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_disponibilidad_sin_horarios_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_ia_solicitudes_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_ia_respuestas_exitosas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_ia_respuestas_fallidas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_ia_modo_seguro_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_notificaciones_enviadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_notificaciones_fallidas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_notificaciones_reintentos_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_outbox_procesadas_total")).isEqualTo(1.0);
		assertThat(counterValue("assistente_outbox_fallidas_total")).isEqualTo(1.0);
	}

	@Test
	void recordsTimersWithTags() {
		BusinessMetrics metrics = new BusinessMetrics(meterRegistry);

		metrics.recordReservaOperacion("crear", 120);
		metrics.recordReservaOperacion("crear", 80);
		metrics.recordReservaOperacion("reprogramar", 200);
		metrics.recordDisponibilidadDuracion(300);
		metrics.recordIaDuracion(400);

		assertThat(
				meterRegistry.timer("assistente_reservas_operaciones_duracion_seconds", "operacion", "crear").count())
				.isEqualTo(2);
		assertThat(meterRegistry.timer("assistente_reservas_operaciones_duracion_seconds", "operacion", "reprogramar")
				.count()).isEqualTo(1);
		assertThat(meterRegistry.timer("assistente_disponibilidad_duracion_seconds").count()).isEqualTo(1);
		assertThat(meterRegistry.timer("assistente_ia_duracion_seconds").count()).isEqualTo(1);
		assertThat(meterRegistry.timer("assistente_disponibilidad_duracion_seconds").totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(300.0);
	}

	@Test
	void sanitizesDynamicTags() {
		BusinessMetrics metrics = new BusinessMetrics(meterRegistry);

		metrics.recordIntencionDetectada("Reservar Hora!");
		metrics.recordIntencionDetectada("reservar_hora!");
		metrics.recordIaDerivacion("OpenAI");
		metrics.recordTareaProgramadaExitosa("Outbox Worker");

		assertThat(
				meterRegistry.counter("assistente_intenciones_detectadas_total", "intencion", "reservar_hora_").count())
				.isEqualTo(2.0);
		assertThat(meterRegistry.counter("assistente_ia_derivaciones_total", "proveedor", "openai").count())
				.isEqualTo(1.0);
		assertThat(
				meterRegistry.counter("assistente_tareas_programadas_exitosas_total", "tarea", "outbox_worker").count())
				.isEqualTo(1.0);
	}

	private double counterValue(String name) {
		return meterRegistry.counter(name).count();
	}
}
