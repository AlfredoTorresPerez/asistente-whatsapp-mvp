package com.asistentewhatsapp.channels.domain;

import java.time.Instant;

public record ChannelDelivery(
        MessageChannelType channelType,
        String externalMessageId,
        String status,
        Instant acceptedAt) {
}
