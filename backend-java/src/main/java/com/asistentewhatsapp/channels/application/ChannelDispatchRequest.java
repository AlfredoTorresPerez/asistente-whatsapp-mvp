package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.MessageChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChannelDispatchRequest(
        @NotNull(message = "businessId es obligatorio")
        UUID businessId,
        @NotNull(message = "channelType es obligatorio")
        MessageChannelType channelType,
        @NotBlank(message = "recipientPhone es obligatorio")
        @Size(max = 30, message = "recipientPhone no puede superar 30 caracteres")
        String recipientPhone,
        @NotBlank(message = "body es obligatorio")
        @Size(max = 1000, message = "body no puede superar 1000 caracteres")
        String body) {
}
