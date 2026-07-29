package com.asistentewhatsapp.content.api.dto;

import java.util.UUID;

public record ContentItemPublicResponse(UUID id, String type, String text, String imageUrl, String status) {
}