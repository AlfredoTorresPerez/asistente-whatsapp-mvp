package com.asistentewhatsapp.dashboard.application;

import com.asistentewhatsapp.dashboard.api.DashboardActivityResponse;
import com.asistentewhatsapp.dashboard.api.DashboardAppointmentResponse;
import com.asistentewhatsapp.dashboard.api.DashboardKpisResponse;
import com.asistentewhatsapp.dashboard.api.DashboardSeriesPointResponse;
import com.asistentewhatsapp.dashboard.api.DashboardSummaryResponse;
import com.asistentewhatsapp.dashboard.infrastructure.DashboardJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DashboardJdbcRepository dashboardJdbcRepository;

    public DashboardService(DashboardJdbcRepository dashboardJdbcRepository) {
        this.dashboardJdbcRepository = dashboardJdbcRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(
            AuthenticatedUser authenticatedUser,
            OffsetDateTime from,
            OffsetDateTime to,
            UUID ownerUserId) {
        DashboardRange dashboardRange = resolveRange(authenticatedUser, from, to);
        DashboardRange todayRange = resolveTodayRange(authenticatedUser);

        DashboardKpisResponse kpis = new DashboardKpisResponse(
                dashboardJdbcRepository.countOpenConversations(
                        authenticatedUser.businessId(),
                        ownerUserId,
                        dashboardRange.from(),
                        dashboardRange.to()),
                dashboardJdbcRepository.countNewProspects(
                        authenticatedUser.businessId(),
                        ownerUserId,
                        dashboardRange.from(),
                        dashboardRange.to()),
                dashboardJdbcRepository.countOpenOrders(
                        authenticatedUser.businessId(),
                        ownerUserId,
                        dashboardRange.from(),
                        dashboardRange.to()),
                dashboardJdbcRepository.countPendingAppointments(
                        authenticatedUser.businessId(),
                        ownerUserId,
                        dashboardRange.from(),
                        dashboardRange.to()));

        List<DashboardSeriesPointResponse> conversationSeries = dashboardJdbcRepository.loadConversationSeries(
                authenticatedUser.businessId(),
                ownerUserId,
                dashboardRange.from(),
                dashboardRange.to());
        List<DashboardSeriesPointResponse> orderSeries = dashboardJdbcRepository.loadOrderSeries(
                authenticatedUser.businessId(),
                ownerUserId,
                dashboardRange.from(),
                dashboardRange.to());
        List<DashboardAppointmentResponse> todayAppointments = dashboardJdbcRepository.loadTodayAppointments(
                authenticatedUser.businessId(),
                ownerUserId,
                todayRange.from(),
                todayRange.to());
        List<DashboardActivityResponse> recentActivity = dashboardJdbcRepository.loadRecentActivity(
                authenticatedUser.businessId(),
                ownerUserId,
                dashboardRange.from(),
                dashboardRange.to());

        return new DashboardSummaryResponse(
                kpis,
                conversationSeries,
                orderSeries,
                todayAppointments,
                recentActivity);
    }

    private DashboardRange resolveRange(
            AuthenticatedUser authenticatedUser,
            OffsetDateTime from,
            OffsetDateTime to) {
        ZoneId zoneId = resolveZone(authenticatedUser);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        OffsetDateTime resolvedTo = to != null
                ? to.withOffsetSameInstant(ZoneOffset.UTC)
                : now.with(LocalTime.MAX).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        OffsetDateTime resolvedFrom = from != null
                ? from.withOffsetSameInstant(ZoneOffset.UTC)
                : now.minusDays(6)
                        .with(LocalTime.MIN)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toOffsetDateTime();

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La solicitud contiene datos invalidos.",
                    Map.of("from", "El rango de fechas es invalido.", "to", "El rango de fechas es invalido."));
        }

        return new DashboardRange(resolvedFrom, resolvedTo);
    }

    private DashboardRange resolveTodayRange(AuthenticatedUser authenticatedUser) {
        ZoneId zoneId = resolveZone(authenticatedUser);
        LocalDate today = LocalDate.now(zoneId);
        ZonedDateTime startOfDay = today.atStartOfDay(zoneId);
        ZonedDateTime endOfDay = today.atTime(LocalTime.MAX).atZone(zoneId);
        return new DashboardRange(
                startOfDay.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                endOfDay.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
    }

    private ZoneId resolveZone(AuthenticatedUser authenticatedUser) {
        try {
            return ZoneId.of(authenticatedUser.timezone());
        } catch (Exception ignored) {
            return ZoneOffset.UTC;
        }
    }

    private record DashboardRange(
            OffsetDateTime from,
            OffsetDateTime to) {
    }
}
