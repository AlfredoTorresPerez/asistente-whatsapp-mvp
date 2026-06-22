package com.asistentewhatsapp.aiagents.application;

import java.util.List;

public final class WhatsAppMessageFormatter {

    private WhatsAppMessageFormatter() {
    }

    public static String temporaryBookingCreated(
            String service,
            String location,
            String date,
            String time,
            String confirmationUrl,
            int expirationMinutes) {
        return "✅ *Reserva temporal creada*\n\n"
                + "Hola, dejé una *reserva temporal* para ti:\n\n"
                + "*Servicio:* " + safe(service, "el servicio") + "\n"
                + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
                + "*Fecha:* " + safe(date, "la fecha solicitada") + "\n"
                + "*Hora:* " + safe(time, "la hora solicitada") + "\n\n"
                + "👉 *Toca o copia este enlace para confirmar tu reserva:*\n\n"
                + safe(confirmationUrl, "") + "\n\n"
                + "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
                + "⏳ *Importante:* este enlace vence en *" + formatDuration(expirationMinutes) + "*.\n"
                + "Si no confirmas a tiempo, el cupo puede liberarse.";
    }

    public static String bookingPreview(String service, String location, String date, String time) {
        return "👀 *Vista previa de reserva*\n\n"
                + "Hay disponibilidad para:\n\n"
                + "*Servicio:* " + safe(service, "el servicio") + "\n"
                + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
                + "*Fecha:* " + safe(date, "la fecha solicitada") + "\n"
                + "*Hora:* " + safe(time, "la hora solicitada") + "\n\n"
                + "Esta vista previa *no creó una reserva temporal ni un enlace real*.\n"
                + "Al enviar la respuesta por WhatsApp se creará la reserva temporal y el enlace de confirmación.";
    }

    public static String bookingConfirmed(String service, String location, String date, String time) {
        return "✅ *Reserva confirmada*\n\n"
                + "Tu hora quedó confirmada:\n\n"
                + "*Servicio:* " + safe(service, "el servicio") + "\n"
                + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
                + "*Fecha:* " + safe(date, "la fecha solicitada") + "\n"
                + "*Hora:* " + safe(time, "la hora solicitada") + "\n\n"
                + "Te esperamos 💆‍♀️";
    }

    public static String confirmationLinkResent(String confirmationUrl, int minutesRemaining) {
        return "🔁 *Reenvío de enlace de confirmación*\n\n"
                + "Aquí tienes nuevamente tu enlace para confirmar la reserva:\n\n"
                + "👉 *Toca o copia este enlace para confirmar tu reserva:*\n\n"
                + safe(confirmationUrl, "") + "\n\n"
                + "_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._\n\n"
                + "⏳ *Importante:* el enlace vence en *" + formatDuration(minutesRemaining) + "*.\n"
                + "Si ya venció, puedo ayudarte a generar una nueva reserva temporal.";
    }

    public static String confirmationLinkExpired() {
        return "⏳ *El enlace de confirmación venció*\n\n"
                + "El enlace anterior ya no está disponible y el cupo pudo liberarse.\n\n"
                + "Puedo ayudarte a revisar disponibilidad nuevamente para crear una nueva reserva temporal.";
    }

    public static String noAvailability(String service, String location, String date, String requestedTime, List<String> alternatives) {
        String base = "⚠️ *Horario no disponible*\n\n"
                + "No encontré disponibilidad para:\n\n"
                + "*Servicio:* " + safe(service, "el servicio") + "\n"
                + "*Sucursal:* " + safe(location, "la sucursal") + "\n"
                + "*Fecha:* " + safe(date, "la fecha solicitada") + "\n"
                + "*Hora solicitada:* " + safe(requestedTime, "la hora solicitada") + "\n";
        if (alternatives == null || alternatives.isEmpty()) {
            return base + "\nNo encontré horarios cercanos disponibles. Puedo revisar otro día u otra sucursal.";
        }
        StringBuilder builder = new StringBuilder(base)
                .append("\nPuedo revisar otros horarios cercanos. Opciones disponibles:\n\n");
        int index = 1;
        for (String alternative : alternatives.stream().distinct().limit(3).toList()) {
            builder.append(index++).append(". ").append(alternative).append("\n");
        }
        builder.append("\n¿Cuál prefieres?");
        return builder.toString();
    }

    public static String askService() {
        return "Claro 😊 ¿Qué servicio específico quieres agendar?\n\n"
                + "Tengo opciones como:\n"
                + "*Limpieza facial profunda*, *depilación bozo*, *rostro*, *axilas*, *piernas* y *bikini*.";
    }

    public static String askLocation() {
        return "Perfecto 😊 ¿En qué sucursal prefieres atenderte?\n\n"
                + "Opciones:\n"
                + "*Providencia*\n"
                + "*Maipú*\n"
                + "*Santiago Centro*";
    }

    public static String askDate() {
        return "Perfecto 😊 ¿Para qué día te gustaría agendar?";
    }

    public static String askTime() {
        return "Perfecto 😊 ¿A qué hora prefieres asistir?";
    }

    public static String cancellationRequest() {
        return "🛑 *Solicitud de cancelación*\n\n"
                + "Puedo ayudarte a cancelar tu reserva.\n\n"
                + "Para hacerlo de forma segura, necesito identificar la cita:\n"
                + "*Sucursal*, *fecha* u *hora* de la reserva.";
    }

    public static String rescheduleRequest() {
        return "🔄 *Solicitud de reprogramación*\n\n"
                + "Puedo ayudarte a reprogramar tu cita.\n\n"
                + "Para identificar la *cita actual*, indícame la fecha de la cita o el servicio.\n\n"
                + "También dime el nuevo *día* u *horario* que prefieres.";
    }

    public static String sensitiveCase() {
        return "🚨 *Te derivaré con una persona del equipo*\n\n"
                + "Lamento que hayas tenido esa reacción.\n\n"
                + "Voy a derivarte con una persona del equipo para ayudarte de inmediato.\n\n"
                + "Si tienes molestias importantes o síntomas intensos, consulta con un profesional de salud.";
    }

    private static String formatDuration(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours == 1 ? "1 hora" : hours + " horas";
        }
        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
