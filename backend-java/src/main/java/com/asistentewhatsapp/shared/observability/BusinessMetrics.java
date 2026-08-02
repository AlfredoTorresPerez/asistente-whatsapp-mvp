package com.asistentewhatsapp.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

	private final MeterRegistry meterRegistry;

	private final Counter whatsappMensajesRecibidos;
	private final Counter whatsappMensajesEnviados;
	private final Counter whatsappMensajesFallidos;
	private final Counter whatsappWebhooksRecibidos;
	private final Counter whatsappWebhooksFirmaInvalida;
	private final Counter whatsappEventosDuplicados;
	private final Counter conversacionesIniciadas;
	private final Counter conversacionesDerivadas;
	private final Counter intencionesAmbiguas;
	private final Counter reservasCreadas;
	private final Counter reservasConfirmadas;
	private final Counter reservasReprogramadas;
	private final Counter reservasCanceladas;
	private final Counter reservasConflicto;
	private final Counter reservasExpiradas;
	private final Counter disponibilidadConsultas;
	private final Counter disponibilidadSinHorarios;
	private final Counter iaSolicitudes;
	private final Counter iaRespuestasExitosas;
	private final Counter iaRespuestasFallidas;
	private final Counter iaModoSeguro;
	private final Counter notificacionesEnviadas;
	private final Counter notificacionesFallidas;
	private final Counter notificacionesReintentos;
	private final Counter outboxProcesadas;
	private final Counter outboxFallidas;
	private final Timer disponibilidadDuracion;
	private final Timer iaDuracion;

	private final ConcurrentMap<String, Counter> intencionesDetectadas = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Counter> iaDerivaciones = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Counter> tareasProgramadasExitosas = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Counter> tareasProgramadasFallidas = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Timer> reservasOperacionesTimers = new ConcurrentHashMap<>();

	public BusinessMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.whatsappMensajesRecibidos = counter("assistente_whatsapp_mensajes_recibidos_total",
				"Total de mensajes de WhatsApp recibidos y procesados");
		this.whatsappMensajesEnviados = counter("assistente_whatsapp_mensajes_enviados_total",
				"Total de mensajes de WhatsApp enviados al cliente");
		this.whatsappMensajesFallidos = counter("assistente_whatsapp_mensajes_fallidos_total",
				"Total de mensajes de WhatsApp que fallaron al enviarse");
		this.whatsappWebhooksRecibidos = counter("assistente_whatsapp_webhooks_recibidos_total",
				"Total de webhooks de WhatsApp recibidos");
		this.whatsappWebhooksFirmaInvalida = counter("assistente_whatsapp_webhooks_firma_invalida_total",
				"Total de webhooks rechazados por firma invalida");
		this.whatsappEventosDuplicados = counter("assistente_whatsapp_eventos_duplicados_total",
				"Total de eventos de WhatsApp duplicados descartados");
		this.conversacionesIniciadas = counter("assistente_conversaciones_iniciadas_total",
				"Total de conversaciones nuevas iniciadas");
		this.conversacionesDerivadas = counter("assistente_conversaciones_derivadas_total",
				"Total de conversaciones derivadas a agente humano");
		this.intencionesAmbiguas = counter("assistente_intenciones_ambiguas_total",
				"Total de intenciones ambiguas detectadas");
		this.reservasCreadas = counter("assistente_reservas_creadas_total", "Total de reservas creadas");
		this.reservasConfirmadas = counter("assistente_reservas_confirmadas_total", "Total de reservas confirmadas");
		this.reservasReprogramadas = counter("assistente_reservas_reprogramadas_total",
				"Total de reservas reprogramadas");
		this.reservasCanceladas = counter("assistente_reservas_canceladas_total", "Total de reservas canceladas");
		this.reservasConflicto = counter("assistente_reservas_conflicto_total",
				"Total de reservas rechazadas por conflicto de horario");
		this.reservasExpiradas = counter("assistente_reservas_expiradas_total", "Total de reservas expiradas");
		this.disponibilidadConsultas = counter("assistente_disponibilidad_consultas_total",
				"Total de consultas de disponibilidad");
		this.disponibilidadSinHorarios = counter("assistente_disponibilidad_sin_horarios_total",
				"Total de consultas de disponibilidad sin horarios disponibles");
		this.iaSolicitudes = counter("assistente_ia_solicitudes_total", "Total de solicitudes al proveedor de IA");
		this.iaRespuestasExitosas = counter("assistente_ia_respuestas_exitosas_total",
				"Total de respuestas exitosas del proveedor de IA");
		this.iaRespuestasFallidas = counter("assistente_ia_respuestas_fallidas_total",
				"Total de respuestas fallidas del proveedor de IA");
		this.iaModoSeguro = counter("assistente_ia_modo_seguro_total", "Total de respuestas emitidas en modo seguro");
		this.notificacionesEnviadas = counter("assistente_notificaciones_enviadas_total",
				"Total de notificaciones enviadas");
		this.notificacionesFallidas = counter("assistente_notificaciones_fallidas_total",
				"Total de notificaciones con error de envio");
		this.notificacionesReintentos = counter("assistente_notificaciones_reintentos_total",
				"Total de reintentos de notificaciones");
		this.outboxProcesadas = counter("assistente_outbox_procesadas_total",
				"Total de respuestas de IA procesadas desde el outbox");
		this.outboxFallidas = counter("assistente_outbox_fallidas_total",
				"Total de respuestas de IA fallidas en el outbox");
		this.disponibilidadDuracion = Timer.builder("assistente_disponibilidad_duracion_seconds")
				.description("Duracion de consultas de disponibilidad").register(meterRegistry);
		this.iaDuracion = Timer.builder("assistente_ia_duracion_seconds")
				.description("Latencia de llamadas al proveedor de IA").register(meterRegistry);
	}

	public void incrementWhatsappMensajesRecibidos() {
		whatsappMensajesRecibidos.increment();
	}

	public void incrementWhatsappMensajesEnviados() {
		whatsappMensajesEnviados.increment();
	}

	public void incrementWhatsappMensajesFallidos() {
		whatsappMensajesFallidos.increment();
	}

	public void incrementWhatsappWebhooksRecibidos() {
		whatsappWebhooksRecibidos.increment();
	}

	public void incrementWhatsappWebhooksFirmaInvalida() {
		whatsappWebhooksFirmaInvalida.increment();
	}

	public void incrementWhatsappEventosDuplicados() {
		whatsappEventosDuplicados.increment();
	}

	public void incrementConversacionesIniciadas() {
		conversacionesIniciadas.increment();
	}

	public void incrementConversacionesDerivadas() {
		conversacionesDerivadas.increment();
	}

	public void recordIntencionDetectada(String intencion) {
		intencionesDetectadas
				.computeIfAbsent(sanitizeTag(intencion),
						key -> Counter.builder("assistente_intenciones_detectadas_total").tag("intencion", key)
								.description("Total de intenciones detectadas por tipo").register(meterRegistry))
				.increment();
	}

	public void incrementIntencionesAmbiguas() {
		intencionesAmbiguas.increment();
	}

	public void incrementReservasCreadas() {
		reservasCreadas.increment();
	}

	public void incrementReservasConfirmadas() {
		reservasConfirmadas.increment();
	}

	public void incrementReservasReprogramadas() {
		reservasReprogramadas.increment();
	}

	public void incrementReservasCanceladas() {
		reservasCanceladas.increment();
	}

	public void incrementReservasConflicto() {
		reservasConflicto.increment();
	}

	public void incrementReservasExpiradas() {
		reservasExpiradas.increment();
	}

	public void recordReservaOperacion(String operacion, long duracionMillis) {
		Timer timer = reservasOperacionesTimers.computeIfAbsent(sanitizeTag(operacion),
				key -> Timer.builder("assistente_reservas_operaciones_duracion_seconds").tag("operacion", key)
						.description("Duracion de operaciones de reserva por tipo").register(meterRegistry));
		timer.record(duracionMillis, TimeUnit.MILLISECONDS);
	}

	public void incrementDisponibilidadConsultas() {
		disponibilidadConsultas.increment();
	}

	public void incrementDisponibilidadSinHorarios() {
		disponibilidadSinHorarios.increment();
	}

	public void recordDisponibilidadDuracion(long duracionMillis) {
		disponibilidadDuracion.record(duracionMillis, TimeUnit.MILLISECONDS);
	}

	public void incrementIaSolicitudes() {
		iaSolicitudes.increment();
	}

	public void incrementIaRespuestasExitosas() {
		iaRespuestasExitosas.increment();
	}

	public void incrementIaRespuestasFallidas() {
		iaRespuestasFallidas.increment();
	}

	public void recordIaDerivacion(String proveedor) {
		iaDerivaciones
				.computeIfAbsent(sanitizeTag(proveedor),
						key -> Counter.builder("assistente_ia_derivaciones_total").tag("proveedor", key)
								.description("Total de derivaciones a proveedor de IA").register(meterRegistry))
				.increment();
	}

	public void incrementIaModoSeguro() {
		iaModoSeguro.increment();
	}

	public void recordIaDuracion(long duracionMillis) {
		iaDuracion.record(duracionMillis, TimeUnit.MILLISECONDS);
	}

	public void incrementNotificacionesEnviadas() {
		notificacionesEnviadas.increment();
	}

	public void incrementNotificacionesFallidas() {
		notificacionesFallidas.increment();
	}

	public void incrementNotificacionesReintentos() {
		notificacionesReintentos.increment();
	}

	public void incrementOutboxProcesadas() {
		outboxProcesadas.increment();
	}

	public void incrementOutboxFallidas() {
		outboxFallidas.increment();
	}

	public void recordTareaProgramadaExitosa(String tarea) {
		tareasProgramadasExitosas.computeIfAbsent(sanitizeTag(tarea),
				key -> Counter.builder("assistente_tareas_programadas_exitosas_total").tag("tarea", key)
						.description("Total de ejecuciones exitosas de tareas programadas").register(meterRegistry))
				.increment();
	}

	public void recordTareaProgramadaFallida(String tarea) {
		tareasProgramadasFallidas.computeIfAbsent(sanitizeTag(tarea),
				key -> Counter.builder("assistente_tareas_programadas_fallidas_total").tag("tarea", key)
						.description("Total de ejecuciones fallidas de tareas programadas").register(meterRegistry))
				.increment();
	}

	private Counter counter(String name, String description) {
		return Counter.builder(name).description(description).register(meterRegistry);
	}

	private String sanitizeTag(String value) {
		if (value == null || value.isBlank()) {
			return "desconocido";
		}
		return value.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_").replaceAll("_+", "_");
	}
}
