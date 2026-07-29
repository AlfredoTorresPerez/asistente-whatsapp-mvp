package com.asistentewhatsapp.administration.api;

import java.util.UUID;

public record AdminRoleResponse(UUID id, String code, String name, String description, long permissionCount) {
}
