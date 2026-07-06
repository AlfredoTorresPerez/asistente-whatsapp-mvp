package com.asistentewhatsapp.landing.api;

import java.util.UUID;

public record PublicServiceBranchResponse(
        UUID id,
        String name,
        String address,
        String commune,
        String phone,
        int professionalCount) {
}
