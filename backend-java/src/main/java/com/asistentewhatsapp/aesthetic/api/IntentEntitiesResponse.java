package com.asistentewhatsapp.aesthetic.api;

public record IntentEntitiesResponse(
        String servicio,
        String producto,
        String fecha,
        String hora,
        String profesional,
        String cliente) {

    public static IntentEntitiesResponse empty() {
        return new IntentEntitiesResponse(null, null, null, null, null, null);
    }
}
