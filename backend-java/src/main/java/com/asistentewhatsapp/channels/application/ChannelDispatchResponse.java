package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.MessageChannelType;
import java.time.Instant;

public record ChannelDispatchResponse(
        MessageChannelType channelType,
        String externalMessageId,
        String status,
        Instant acceptedAt) {
}
