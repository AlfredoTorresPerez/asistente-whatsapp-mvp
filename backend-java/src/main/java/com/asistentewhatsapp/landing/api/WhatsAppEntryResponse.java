package com.asistentewhatsapp.landing.api;

public record WhatsAppEntryResponse(
        String waUrl,
        String phoneNumber,
        String displayPhoneNumber,
        String prefilledMessage,
        String locationCode,
        String locationName) {
}
