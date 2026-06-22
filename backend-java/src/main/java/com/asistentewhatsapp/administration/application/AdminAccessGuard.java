package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

final class AdminAccessGuard {

    private AdminAccessGuard() {
    }

    static void requireOwnerOrAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.roles().stream().noneMatch(AdminAccessGuard::isAllowedRole)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ADMIN_ACCESS_DENIED",
                    "No tienes permisos para administrar usuarios o seguridad.");
        }
    }

    static void requireOwnerAdminOrSupervisor(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.roles().stream().noneMatch(AdminAccessGuard::isReadAllowedRole)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ADMIN_ACCESS_DENIED",
                    "No tienes permisos para consultar informacion administrativa.");
        }
    }

    private static boolean isAllowedRole(String role) {
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }

    private static boolean isReadAllowedRole(String role) {
        return isAllowedRole(role) || "SUPERVISOR".equals(role);
    }
}
