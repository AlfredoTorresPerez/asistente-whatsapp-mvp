package com.asistentewhatsapp.aiagents.domain;

public enum AgentType {
    RECEPTION("Recepción"),
    SALES("Ventas"),
    BOOKING("Agenda"),
    SUPPORT("Soporte"),
    PAYMENTS("Pagos"),
    FOLLOW_UP("Seguimiento"),
    KNOWLEDGE("Conocimiento"),
    HUMAN_HANDOFF("Derivación Humana");

    private final String displayName;

    AgentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
