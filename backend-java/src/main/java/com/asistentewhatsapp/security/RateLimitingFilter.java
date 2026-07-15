package com.asistentewhatsapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-1)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private RateLimitingService rateLimitingService;

    public RateLimitingFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);

        if (rateLimitingService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitingService.LimitType limitType = resolveLimitType(path, method);

        if (limitType != null) {
            String key = clientIp;
            if (path.contains("/auth/login") || path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
                key = clientIp;
            }

            if (!rateLimitingService.tryConsume(key, limitType)) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(limitType.window.toSeconds()));
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Demasiadas solicitudes. Intenta de nuevo mas tarde.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitingService.LimitType resolveLimitType(String path, String method) {
        if (path.contains("/auth/login")) {
            return RateLimitingService.LimitType.LOGIN;
        }
        if (path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
            return RateLimitingService.LimitType.PASSWORD_RESET;
        }
        if (path.contains("/webhook") || path.contains("/integrations/")) {
            return RateLimitingService.LimitType.WEBHOOK;
        }
        if (path.startsWith("/api/") && !"GET".equalsIgnoreCase(method)) {
            return RateLimitingService.LimitType.API;
        }
        return null;
    }

}
