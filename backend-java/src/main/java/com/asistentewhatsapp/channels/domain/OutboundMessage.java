package com.asistentewhatsapp.channels.domain;

import java.util.UUID;

public record OutboundMessage(
        UUID businessId,
        String recipientPhone,
        String body) {
}
