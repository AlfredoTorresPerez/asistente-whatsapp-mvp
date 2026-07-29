package com.asistentewhatsapp.multisite.api;

import java.util.UUID;

public record UpdateChannelLocationRequest(UUID locationId, String routingMode) {
}
