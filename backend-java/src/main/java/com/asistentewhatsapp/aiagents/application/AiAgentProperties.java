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
     * Permite que el webhook envie automaticamente la respuesta sugerida.
     * Por seguridad queda desactivado por defecto.
     */
    private boolean autoReplyEnabled = false;

    /**
     * Si esta activo, registra contexto y logs aunque autoReplyEnabled sea false.
     */
    private boolean auditEnabled = true;

    private double defaultConfidence = 0.78;

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

    public double defaultConfidence() {
        return defaultConfidence;
    }

    public void setDefaultConfidence(double defaultConfidence) {
        this.defaultConfidence = defaultConfidence;
    }
}
