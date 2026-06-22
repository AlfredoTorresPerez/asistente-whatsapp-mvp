package com.asistentewhatsapp.multisite.api;

import java.util.UUID;

public record MultisiteChannelResponse(
        UUID channelId,
        String channelType,
        String providerName,
        String status,
        String phoneNumber,
        UUID locationId,
        String locationName,
        String routingMode,
        boolean active) {
}
