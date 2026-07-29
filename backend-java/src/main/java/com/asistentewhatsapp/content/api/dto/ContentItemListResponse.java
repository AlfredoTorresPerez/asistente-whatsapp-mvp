package com.asistentewhatsapp.content.api.dto;

import java.util.List;

public record ContentItemListResponse(List<ContentItemSummaryResponse> items, int page, int size, long totalItems,
		int totalPages, ContentItemStatsResponse stats) {
}