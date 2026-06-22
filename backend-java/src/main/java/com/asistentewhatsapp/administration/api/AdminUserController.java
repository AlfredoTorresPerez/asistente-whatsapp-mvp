package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.AdminUserService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/api/v1/admin/users")
    public PagedResponse<AdminUserResponse> listUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return adminUserService.listUsers(authenticatedUser, page, size, search, role, status);
    }

    @GetMapping("/api/v1/admin/users/{userId}")
    public AdminUserResponse getUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID userId) {
        return adminUserService.getUser(authenticatedUser, userId);
    }

    @PostMapping(value = "/api/v1/admin/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AdminUserResponse createUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AdminUserRequest request) {
        return adminUserService.createUser(authenticatedUser, request);
    }

    @PatchMapping(value = "/api/v1/admin/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AdminUserResponse updateUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserRequest request) {
        return adminUserService.updateUser(authenticatedUser, userId, request);
    }

    @GetMapping("/api/v1/admin/roles")
    public List<AdminRoleResponse> listRoles(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return adminUserService.listRoles(authenticatedUser);
    }
}
