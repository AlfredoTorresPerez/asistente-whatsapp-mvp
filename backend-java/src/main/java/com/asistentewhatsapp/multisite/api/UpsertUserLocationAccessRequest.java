package com.asistentewhatsapp.multisite.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpsertUserLocationAccessRequest(
        @NotNull UUID userId,
        @NotNull UUID locationId,
        @Size(max = 30) String roleScope,
        Boolean canViewConversations,
        Boolean canManageBookings,
        Boolean canManageOrders,
        Boolean canManageCatalog,
        Boolean canViewReports,
        Boolean active) {
}
