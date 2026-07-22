package com.asistentewhatsapp.security;

import java.util.List;
import org.springframework.util.AntPathMatcher;

public final class SecurityPublicPaths {

    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/health",
        "/actuator/health",
        "/actuator/prometheus",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/reset-password/validate",
        "/api/auth/login",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/reset-password/validate",
        "/api/v1/integrations/whatsapp-web/webhook",
        "/api/v1/integrations/whatsapp-cloud/webhook",
        "/api/v1/integrations/booking-payments/webhook",
        "/api/webhooks/whatsapp-web/messages",
        "/api/v1/public/booking-confirmations/**",
        "/api/v1/public/booking-reschedules/**",
        "/api/v1/public/booking-cancellations/**",
        "/api/v1/public/booking-payments/**",
        "/api/v1/public/customer-bookings/**",
        "/api/v1/public/landing/**",
        "/api/v1/calendar-integrations/google/callback",
        "/api/v1/calendar-integrations/outlook/callback",
    };

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private SecurityPublicPaths() {}

    public static boolean isPublicPath(String path) {
        return List.of(PUBLIC_ENDPOINTS).stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
