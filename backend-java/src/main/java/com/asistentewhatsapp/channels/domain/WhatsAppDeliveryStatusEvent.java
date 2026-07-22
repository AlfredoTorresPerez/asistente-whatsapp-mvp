package com.asistentewhatsapp.channels.domain;

import java.time.OffsetDateTime;

public record WhatsAppDeliveryStatusEvent(
        String externalMessageId,
        String businessId,
        String channelAccountId,
        WhatsAppDeliveryStatus status,
        OffsetDateTime timestamp,
        String errorCode,
        String errorTitle,
        String errorDetails) {
}
