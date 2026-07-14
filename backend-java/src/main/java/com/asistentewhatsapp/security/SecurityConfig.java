package com.asistentewhatsapp.security;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    @Order(0)
    SecurityFilterChain publicBookingConfirmationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/v1/public/booking-confirmations/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/v1/public/booking-confirmations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/booking-confirmations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/booking-confirmations/**").permitAll()
                        .anyRequest().permitAll())
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicCustomerBookingsSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/v1/public/customer-bookings/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(SecurityPublicPaths.PUBLIC_ENDPOINTS)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(SecurityPublicPaths.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/**")
                        .hasAnyRole("OWNER", "ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/admin/**")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/company", "/api/businesses/current")
                        .hasAnyRole("OWNER", "ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/company", "/api/businesses/current")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/configuration/whatsapp/**")
                        .hasAnyRole("OWNER", "ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/configuration/whatsapp/**")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/whatsapp-web/status", "/api/channels/whatsapp-web/status")
                        .hasAnyRole("OWNER", "ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/whatsapp-web/**", "/api/channels/whatsapp-web/**")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/security/**", "/api/security/**")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/security/audit-log", "/api/security/audit-log")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/system/status")
                        .hasAnyRole("OWNER", "ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppSecurityProperties appSecurityProperties) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(appSecurityProperties.getCorsAllowedOrigins());
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Correlation-Id"));
        corsConfiguration.setExposedHeaders(List.of("Location", "X-Correlation-Id"));
        corsConfiguration.setAllowCredentials(false);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
