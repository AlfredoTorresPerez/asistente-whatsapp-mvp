package com.asistentewhatsapp.reports.application;

import com.asistentewhatsapp.reports.api.ReportsAppointmentDistributionPoint;
import com.asistentewhatsapp.reports.api.ReportsAppointmentPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsChannelResponse;
import com.asistentewhatsapp.reports.api.ReportsConversationPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsFunnelStageResponse;
import com.asistentewhatsapp.reports.api.ReportsKpiItem;
import com.asistentewhatsapp.reports.api.ReportsProspectsResponse;
import com.asistentewhatsapp.reports.api.ReportsSummaryResponse;
import com.asistentewhatsapp.reports.infrastructure.ReportsJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

    private final ReportsJdbcRepository reportsJdbcRepository;

    public ReportsService(ReportsJdbcRepository reportsJdbcRepository) {
        this.reportsJdbcRepository = reportsJdbcRepository;
    }

    @Transactional(readOnly = true)
    public ReportsSummaryResponse getSummary(
            AuthenticatedUser authenticatedUser,
            LocalDate from,
            LocalDate to,
            UUID locationId,
            UUID professionalId,
            UUID serviceId,
            String bookingStatus,
            UUID ownerUserId,
            int page,
            int size) {
        ZoneId zoneId = resolveZone(authenticatedUser);
        ReportRange range = resolveRange(zoneId, from, to);
        ReportRange previousRange = previousPeriod(range);

        UUID businessId = authenticatedUser.businessId();
        OffsetDateTime rFrom = range.from();
        OffsetDateTime rTo = range.to();
        OffsetDateTime pFrom = previousRange.from();
        OffsetDateTime pTo = previousRange.to();

        List<ReportsKpiItem> kpis = List.of(
                reportsJdbcRepository.buildConversationsKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo),
                reportsJdbcRepository.buildProspectsKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo),
                reportsJdbcRepository.buildAppointmentsCreatedKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo),
                reportsJdbcRepository.buildConfirmedAppointmentsKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo),
                reportsJdbcRepository.buildResponseRateKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo),
                reportsJdbcRepository.buildConversionRateKpi(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                        rFrom, rTo, pFrom, pTo));

        List<ReportsChannelResponse> channels = reportsJdbcRepository.loadChannelDistribution(
                businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

        List<ReportsConversationPerformancePoint> conversationPerf =
                reportsJdbcRepository.loadConversationPerformance(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

        List<ReportsAppointmentPerformancePoint> appointmentPerf =
                reportsJdbcRepository.loadAppointmentPerformance(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

        List<ReportsAppointmentDistributionPoint> distribution =
                reportsJdbcRepository.loadAppointmentDistribution(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

        List<ReportsFunnelStageResponse> funnel =
                reportsJdbcRepository.loadConversionFunnel(
                        businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

        ReportsProspectsResponse prospects = reportsJdbcRepository.loadProspects(
                businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
                rFrom, rTo, null, page, size);

        return new ReportsSummaryResponse(kpis, channels, conversationPerf, distribution, appointmentPerf, funnel, prospects);
    }

    private ReportRange resolveRange(ZoneId zoneId, LocalDate from, LocalDate to) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        OffsetDateTime resolvedTo = to != null
                ? to.atTime(LocalTime.MAX).atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime()
                : now.with(LocalTime.MAX).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime resolvedFrom = from != null
                ? from.atStartOfDay(zoneId).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime()
                : now.minusDays(29).with(LocalTime.MIN).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "La solicitud contiene datos invalidos.",
                    Map.of("from", "El rango de fechas es invalido.", "to", "El rango de fechas es invalido."));
        }
        return new ReportRange(resolvedFrom, resolvedTo);
    }

    private ReportRange previousPeriod(ReportRange current) {
        long days = ChronoUnit.DAYS.between(current.from(), current.to()) + 1;
        OffsetDateTime prevTo = current.from().minusNanos(1);
        OffsetDateTime prevFrom = prevTo.minusDays(days - 1).with(LocalTime.MIN).withOffsetSameInstant(ZoneOffset.UTC);
        return new ReportRange(prevFrom, prevTo);
    }

    private ZoneId resolveZone(AuthenticatedUser authenticatedUser) {
        try {
            return ZoneId.of(authenticatedUser.timezone());
        } catch (Exception ignored) {
            return ZoneOffset.UTC;
        }
    }

    private record ReportRange(OffsetDateTime from, OffsetDateTime to) {
    }
}
