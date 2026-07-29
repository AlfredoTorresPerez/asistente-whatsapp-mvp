package com.asistentewhatsapp.aiagents.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.agents")
public class AiAgentProperties {

	/**
	 * Activa el registro de decisiones del coordinador.
	 */
	private boolean enabled = true;

	/**
	 * Permite que el webhook envie automaticamente la respuesta sugerida. Por
	 * seguridad queda desactivado por defecto.
	 */
	private boolean autoReplyEnabled = false;

	/**
	 * Si esta activo, registra contexto y logs aunque autoReplyEnabled sea false.
	 */
	private boolean auditEnabled = true;

	/**
	 * Modo seguro: cuando esta activo, la IA procesa y genera respuestas pero NO
	 * las envía realmente a WhatsApp. Las respuestas se registran en logs y BD para
	 * validación.
	 */
	private boolean safeModeEnabled = false;

	private double defaultConfidence = 0.78;

	private long outboxWorkerIntervalMs = 5000;

	private long outboxBatchSize = 10;

	private long outboxProcessingTimeoutMs = 120000;

	private long outboxRetryBaseDelayMs = 30000;

	private long outboxRetryMaxDelayMs = 900000;

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean autoReplyEnabled() {
		return autoReplyEnabled;
	}

	public void setAutoReplyEnabled(boolean autoReplyEnabled) {
		this.autoReplyEnabled = autoReplyEnabled;
	}

	public boolean auditEnabled() {
		return auditEnabled;
	}

	public void setAuditEnabled(boolean auditEnabled) {
		this.auditEnabled = auditEnabled;
	}

	public boolean safeModeEnabled() {
		return safeModeEnabled;
	}

	public void setSafeModeEnabled(boolean safeModeEnabled) {
		this.safeModeEnabled = safeModeEnabled;
	}

	public double defaultConfidence() {
		return defaultConfidence;
	}

	public void setDefaultConfidence(double defaultConfidence) {
		this.defaultConfidence = defaultConfidence;
	}

	public long getOutboxWorkerIntervalMs() {
		return outboxWorkerIntervalMs;
	}

	public void setOutboxWorkerIntervalMs(long outboxWorkerIntervalMs) {
		this.outboxWorkerIntervalMs = outboxWorkerIntervalMs;
	}

	public long getOutboxBatchSize() {
		return outboxBatchSize;
	}

	public void setOutboxBatchSize(long outboxBatchSize) {
		this.outboxBatchSize = outboxBatchSize;
	}

	public long getOutboxProcessingTimeoutMs() {
		return outboxProcessingTimeoutMs;
	}

	public void setOutboxProcessingTimeoutMs(long outboxProcessingTimeoutMs) {
		this.outboxProcessingTimeoutMs = outboxProcessingTimeoutMs;
	}

	public long getOutboxRetryBaseDelayMs() {
		return outboxRetryBaseDelayMs;
	}

	public void setOutboxRetryBaseDelayMs(long outboxRetryBaseDelayMs) {
		this.outboxRetryBaseDelayMs = outboxRetryBaseDelayMs;
	}

	public long getOutboxRetryMaxDelayMs() {
		return outboxRetryMaxDelayMs;
	}

	public void setOutboxRetryMaxDelayMs(long outboxRetryMaxDelayMs) {
		this.outboxRetryMaxDelayMs = outboxRetryMaxDelayMs;
	}
}
