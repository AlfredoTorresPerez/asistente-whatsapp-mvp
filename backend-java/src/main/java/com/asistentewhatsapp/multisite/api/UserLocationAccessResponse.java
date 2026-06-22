package com.asistentewhatsapp.multisite.api;

import java.util.UUID;

public record UserLocationAccessResponse(
        UUID userId,
        String userName,
        String email,
        UUID locationId,
        String locationName,
        String roleScope,
        boolean canViewConversations,
        boolean canManageBookings,
        boolean canManageOrders,
        boolean canManageCatalog,
        boolean canViewReports,
        boolean active) {
}
