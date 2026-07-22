package com.asistentewhatsapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-3)
public class PublicEndpointCsrfFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/v1/public/booking-confirmations/",
            "/api/v1/public/customer-bookings/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isProtected = PROTECTED_PATHS.stream().anyMatch(path::contains);
        boolean isMutation = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);

        if (isProtected && isMutation) {
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            if ((origin == null || origin.isBlank()) && (referer == null || referer.isBlank())) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":\"CSRF_REQUIRED\",\"message\":\"Se requiere header Origin o Referer para esta solicitud.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
