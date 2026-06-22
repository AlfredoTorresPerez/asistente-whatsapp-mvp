package com.asistentewhatsapp.configuration.api;

public record WhatsAppConfigurationChannelResponse(
        String channelName,
        String phoneNumber,
        String channelType,
        String businessHours,
        boolean automaticResponsesEnabled) {
}
