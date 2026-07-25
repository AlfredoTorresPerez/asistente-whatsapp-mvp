package com.asistentewhatsapp.content.api.dto;

public record ContentItemListRequest(
        int page,
        int size,
        String search,
        String type,
        String status
) {
    public int page() {
        return page >= 0 ? page : 0;
    }

    public int size() {
        return Math.min(Math.max(size, 1), 100);
    }

    public String status() {
        if (status == null || status.isBlank()) return null;
        String upper = status.toUpperCase();
        if ("ACTIVE".equals(upper) || "INACTIVE".equals(upper)) return upper;
        return null;
    }
}