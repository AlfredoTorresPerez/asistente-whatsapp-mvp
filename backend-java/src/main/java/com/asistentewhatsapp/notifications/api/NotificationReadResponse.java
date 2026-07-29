package com.asistentewhatsapp.notifications.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationReadResponse(UUID id, String status, OffsetDateTime readAt) {
}
