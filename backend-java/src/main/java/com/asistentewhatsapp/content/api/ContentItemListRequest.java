package com.asistentewhatsapp.content.api;

public record ContentItemListRequest(
        String type,
        String status,
        int page,
        int size
) {
}