package com.asistentewhatsapp.landing.api;

import java.util.UUID;

public record LandingLocationItemResponse(
        UUID id,
        String name,
        String address,
        String city,
        String commune,
        String phone,
        String whatsappNumber,
        String timezone) {
}
