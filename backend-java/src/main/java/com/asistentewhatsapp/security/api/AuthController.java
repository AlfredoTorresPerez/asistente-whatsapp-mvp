package com.asistentewhatsapp.security.api;

import com.asistentewhatsapp.security.application.AuthService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.StatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/api/v1/auth/login", "/api/auth/login"})
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        return authService.login(request, httpServletRequest.getRemoteAddr());
    }

    @GetMapping({"/api/v1/auth/me", "/api/auth/me"})
    public AuthUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return authService.me(authenticatedUser);
    }

    @PostMapping({"/api/v1/auth/logout", "/api/auth/logout"})
    public StatusResponse logout(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return authService.logout(authenticatedUser);
    }

    @PostMapping({"/api/v1/auth/forgot-password", "/api/auth/forgot-password"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @GetMapping({"/api/v1/auth/reset-password/validate", "/api/auth/reset-password/validate"})
    public ResetPasswordValidationResponse validateResetPasswordToken(@RequestParam("token") String token) {
        return authService.validateResetPasswordToken(token);
    }

    @PostMapping({"/api/v1/auth/reset-password", "/api/auth/reset-password"})
    public StatusResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}

