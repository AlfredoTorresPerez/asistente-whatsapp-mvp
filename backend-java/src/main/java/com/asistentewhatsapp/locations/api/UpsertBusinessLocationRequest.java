package com.asistentewhatsapp.locations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertBusinessLocationRequest(
        @NotBlank(message = "code es obligatorio")
        @Size(max = 50, message = "code no puede superar 50 caracteres")
        String code,
        @NotBlank(message = "name es obligatorio")
        @Size(max = 150, message = "name no puede superar 150 caracteres")
        String name,
        @Size(max = 255, message = "address no puede superar 255 caracteres")
        String address,
        @Size(max = 120, message = "city no puede superar 120 caracteres")
        String city,
        @Size(max = 120, message = "commune no puede superar 120 caracteres")
        String commune,
        @Size(max = 30, message = "phone no puede superar 30 caracteres")
        String phone,
        @Size(max = 30, message = "whatsappNumber no puede superar 30 caracteres")
        String whatsappNumber,
        @Size(max = 60, message = "timezone no puede superar 60 caracteres")
        String timezone,
        Boolean active) {
}
