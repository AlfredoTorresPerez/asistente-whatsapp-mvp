package com.asistentewhatsapp.content.api.dto;

public record ContentStatsResponse(
        long total,
        long active,
        long inactive,
        long withoutImage
) {
}