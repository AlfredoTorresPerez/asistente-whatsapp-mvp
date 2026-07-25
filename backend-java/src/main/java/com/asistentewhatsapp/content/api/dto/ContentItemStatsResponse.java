package com.asistentewhatsapp.content.api.dto;

public record ContentItemStatsResponse(
        long total,
        long active,
        long inactive,
        long withoutImage
) {
}