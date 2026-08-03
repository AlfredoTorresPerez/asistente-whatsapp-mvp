package com.asistentewhatsapp.reports.application;

import com.asistentewhatsapp.reports.api.ReportsAppointmentDistributionPoint;
import com.asistentewhatsapp.reports.api.ReportsAppointmentPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsChannelResponse;
import com.asistentewhatsapp.reports.api.ReportsConversationPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsFunnelStageResponse;
import com.asistentewhatsapp.reports.api.ReportsKpiItem;
import com.asistentewhatsapp.reports.api.ReportsOccupancyResponse;
import com.asistentewhatsapp.reports.api.ReportsPeriodResponse;
import com.asistentewhatsapp.reports.api.ReportsProspectsResponse;
import com.asistentewhatsapp.reports.api.ReportsServiceDemandResponse;
import com.asistentewhatsapp.reports.api.ReportsSummaryResponse;
import com.asistentewhatsapp.reports.infrastructure.ReportsJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

	private final ReportsJdbcRepository reportsJdbcRepository;

	public ReportsService(ReportsJdbcRepository reportsJdbcRepository) {
		this.reportsJdbcRepository = reportsJdbcRepository;
	}

	@Transactional(readOnly = true)
	public ReportsSummaryResponse getSummary(AuthenticatedUser authenticatedUser, LocalDate from, LocalDate to,
			UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, int page,
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
				reportsJdbcRepository.buildConversationsKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildProspectsKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildAppointmentsCreatedKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildConfirmedAppointmentsKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildResponseRateKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildConversionRateKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo));

		List<ReportsKpiItem> operationalKpis = List.of(
				reportsJdbcRepository.buildAvailableHoursKpi(businessId, locationId, professionalId, rFrom, rTo, pFrom,
						pTo),
				reportsJdbcRepository.buildReservedHoursKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildCancellationsKpi(businessId, locationId, professionalId, serviceId,
						ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildReschedulesKpi(businessId, locationId, professionalId, serviceId,
						ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildNoShowsKpi(businessId, locationId, professionalId, serviceId, ownerUserId,
						rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildConfirmationsKpi(businessId, locationId, professionalId, serviceId,
						ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildAverageResponseMinutesKpi(businessId, locationId, ownerUserId, rFrom, rTo,
						pFrom, pTo),
				reportsJdbcRepository.buildConversationToBookingKpi(businessId, locationId, professionalId, serviceId,
						ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildLeadToBookingKpi(businessId, locationId, professionalId, serviceId,
						ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildEstimatedRevenueKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildDepositsKpi(businessId, locationId, professionalId, serviceId, ownerUserId,
						rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildPendingBalancesKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildNewCustomersKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildRecurringCustomersKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo),
				reportsJdbcRepository.buildRetentionKpi(businessId, locationId, professionalId, serviceId,
						bookingStatus, ownerUserId, rFrom, rTo, pFrom, pTo));

		List<ReportsOccupancyResponse> occupancyByProfessional = reportsJdbcRepository.loadOccupancyByProfessional(
				businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);
		List<ReportsOccupancyResponse> occupancyByRoom = reportsJdbcRepository.loadOccupancyByRoom(businessId,
				locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);
		List<ReportsOccupancyResponse> occupancyByLocation = reportsJdbcRepository.loadOccupancyByLocation(businessId,
				locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);
		List<ReportsServiceDemandResponse> topServices = reportsJdbcRepository.loadTopServices(businessId, locationId,
				professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		List<ReportsChannelResponse> channels = reportsJdbcRepository.loadChannelDistribution(businessId, locationId,
				professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		List<ReportsConversationPerformancePoint> conversationPerf = reportsJdbcRepository.loadConversationPerformance(
				businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		List<ReportsAppointmentPerformancePoint> appointmentPerf = reportsJdbcRepository.loadAppointmentPerformance(
				businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		List<ReportsAppointmentDistributionPoint> distribution = reportsJdbcRepository.loadAppointmentDistribution(
				businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		List<ReportsFunnelStageResponse> funnel = reportsJdbcRepository.loadConversionFunnel(businessId, locationId,
				professionalId, serviceId, bookingStatus, ownerUserId, rFrom, rTo);

		ReportsProspectsResponse prospects = reportsJdbcRepository.loadProspects(businessId, locationId, professionalId,
				serviceId, bookingStatus, ownerUserId, rFrom, rTo, null, page, size);

		ReportsPeriodResponse period = new ReportsPeriodResponse(range.localFrom(), range.localTo(),
				previousRange.localFrom(), previousRange.localTo(), zoneId.getId());

		return new ReportsSummaryResponse(period, kpis, operationalKpis, occupancyByProfessional, occupancyByRoom,
				occupancyByLocation, topServices, channels, conversationPerf, distribution, appointmentPerf, funnel,
				prospects);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> exportCsv(AuthenticatedUser authenticatedUser, LocalDate from, LocalDate to,
			UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, int page,
			int size) {
		if (!canExportReports(authenticatedUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso para exportar reportes.",
					Map.of());
		}
		ReportsSummaryResponse summary = getSummary(authenticatedUser, from, to, locationId, professionalId, serviceId,
				bookingStatus, ownerUserId, page, size);
		byte[] csv = buildCsv(summary).getBytes(StandardCharsets.UTF_8);
		String filename = "reporte_" + summary.period().from() + "_" + summary.period().to() + ".csv";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv);
	}

	private ReportRange resolveRange(ZoneId zoneId, LocalDate from, LocalDate to) {
		ZonedDateTime now = ZonedDateTime.now(zoneId);
		LocalDate localTo = to != null ? to : now.toLocalDate();
		LocalDate localFrom = from != null ? from : localTo.minusDays(29);
		OffsetDateTime resolvedTo = localTo.atTime(LocalTime.MAX).atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC)
				.toOffsetDateTime();
		OffsetDateTime resolvedFrom = localFrom.atStartOfDay(zoneId).withZoneSameInstant(ZoneOffset.UTC)
				.toOffsetDateTime();
		if (resolvedFrom.isAfter(resolvedTo)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos.",
					Map.of("from", "El rango de fechas es invalido.", "to", "El rango de fechas es invalido."));
		}
		return new ReportRange(localFrom, localTo, resolvedFrom, resolvedTo);
	}

	private ReportRange previousPeriod(ReportRange current) {
		long days = ChronoUnit.DAYS.between(current.localFrom(), current.localTo()) + 1;
		OffsetDateTime prevTo = current.from().minusNanos(1);
		OffsetDateTime prevFrom = prevTo.minusDays(days - 1).with(LocalTime.MIN).withOffsetSameInstant(ZoneOffset.UTC);
		return new ReportRange(current.localFrom().minusDays(days), current.localFrom().minusDays(1), prevFrom, prevTo);
	}

	private ZoneId resolveZone(AuthenticatedUser authenticatedUser) {
		try {
			return ZoneId.of(authenticatedUser.timezone());
		} catch (Exception ignored) {
			return ZoneOffset.UTC;
		}
	}

	private boolean canExportReports(AuthenticatedUser authenticatedUser) {
		return authenticatedUser.hasPermission("ALL") || authenticatedUser.hasPermission("REPORTS_EXPORT")
				|| authenticatedUser.hasRole("OWNER") || authenticatedUser.hasRole("ADMIN");
	}

	private String buildCsv(ReportsSummaryResponse summary) {
		List<String> rows = new ArrayList<>();
		rows.add("Reporte del " + formatDate(summary.period().from()) + " al " + formatDate(summary.period().to()));
		rows.add("Zona horaria," + csv(summary.period().timezone()));
		rows.add("");
		appendKpis(rows, "INDICADORES GENERALES", summary.kpis());
		appendKpis(rows, "INDICADORES OPERATIVOS", summary.operationalKpis());
		appendOccupancy(rows, "OCUPACION POR PROFESIONAL", summary.occupancyByProfessional());
		appendOccupancy(rows, "OCUPACION POR CABINA", summary.occupancyByRoom());
		appendOccupancy(rows, "OCUPACION POR SUCURSAL", summary.occupancyByLocation());
		rows.add("SERVICIOS MAS SOLICITADOS");
		rows.add("Servicio,Citas,Ingresos estimados");
		for (ReportsServiceDemandResponse service : summary.topServices()) {
			rows.add(String.join(",", csv(service.serviceName()), csv(service.bookings()),
					csv(service.estimatedRevenue())));
		}
		rows.add("");
		rows.add("PROSPECTOS");
		rows.add(
				"Nombre,Telefono,Ultimo contacto,Etapa,Responsable,Proxima cita,Sucursal,Servicio de interes,Estado atencion");
		for (var prospect : summary.prospects().items()) {
			rows.add(String.join(",", csv(prospect.name()), csv(prospect.phone()),
					csv(formatDate(prospect.lastContact())), csv(prospect.stage()), csv(prospect.responsible()),
					csv(formatDate(prospect.nextAppointment())), csv(prospect.location()),
					csv(prospect.serviceInterest()), csv(prospect.attentionStatus())));
		}
		return String.join("\n", rows);
	}

	private void appendKpis(List<String> rows, String title, List<ReportsKpiItem> kpis) {
		rows.add(title);
		rows.add("Indicador,Valor actual,Valor anterior,Variacion,Unidad");
		for (ReportsKpiItem kpi : kpis) {
			rows.add(String.join(",", csv(kpi.label()), csv(kpi.currentValue()), csv(kpi.previousValue()),
					csv(kpi.variationPercent() == null ? "Sin periodo anterior" : kpi.variationPercent()),
					csv(kpi.valueType())));
		}
		rows.add("");
	}

	private void appendOccupancy(List<String> rows, String title, List<ReportsOccupancyResponse> items) {
		rows.add(title);
		rows.add("Nombre,Horas disponibles,Horas reservadas,Ocupacion");
		for (ReportsOccupancyResponse item : items) {
			rows.add(String.join(",", csv(item.name()), csv(minutesToHours(item.availableMinutes())),
					csv(minutesToHours(item.reservedMinutes())),
					csv(item.occupancyPercent() == null ? "Sin horas configuradas" : item.occupancyPercent() + "%")));
		}
		rows.add("");
	}

	private String csv(Object value) {
		String text = value == null ? "" : String.valueOf(value);
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}

	private String formatDate(LocalDate value) {
		return value == null ? "" : value.getDayOfMonth() + "-" + value.getMonthValue() + "-" + value.getYear();
	}

	private String formatDate(OffsetDateTime value) {
		if (value == null)
			return "";
		return formatDate(value.toLocalDate());
	}

	private double minutesToHours(long minutes) {
		return Math.round((minutes / 60.0) * 10.0) / 10.0;
	}

	private record ReportRange(LocalDate localFrom, LocalDate localTo, OffsetDateTime from, OffsetDateTime to) {
	}
}
