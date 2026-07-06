package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingReceiptService {

    private static final String RECEIPT_STATUS_GENERATED = "GENERATED";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingReceiptService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID generateReceipt(UUID businessId, UUID bookingId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("bookingId", bookingId);
        var booking = jdbcTemplate.query(
                """
                        select b.subject, c.display_name as customer_name, c.email as customer_email,
                               c.phone as customer_phone, b.starts_at, b.duration_minutes,
                               bl.name as location_name, s.name as service_name,
                               p.full_name as professional_name, r.name as room_name
                        from booking b
                        join customer c on c.id = b.customer_id and c.business_id = b.business_id
                        left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
                        left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
                        left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
                        left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
                        where b.business_id = :businessId and b.id = :bookingId
                        """,
                params,
                (rs, rowNum) -> Map.of(
                        "subject", rs.getString("subject"),
                        "customerName", rs.getString("customer_name"),
                        "customerEmail", rs.getString("customer_email"),
                        "customerPhone", rs.getString("customer_phone"),
                        "startsAt", rs.getObject("starts_at", OffsetDateTime.class),
                        "durationMinutes", rs.getInt("duration_minutes"),
                        "locationName", rs.getString("location_name"),
                        "serviceName", rs.getString("service_name"),
                        "professionalName", rs.getString("professional_name"),
                        "roomName", rs.getString("room_name")));
        if (booking.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND",
                    "No se encontro la reserva para generar el comprobante.", Map.of());
        }
        UUID receiptId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String receiptNumber = "RCP-" + bookingId.toString().substring(0, 8).toUpperCase()
                + "-" + now.toEpochSecond();
        jdbcTemplate.update(
                """
                        insert into booking_receipt (id, business_id, booking_id, receipt_number, status, generated_at)
                        values (:id, :businessId, :bookingId, :receiptNumber, :status, :generatedAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", receiptId)
                        .addValue("businessId", businessId)
                        .addValue("bookingId", bookingId)
                        .addValue("receiptNumber", receiptNumber)
                        .addValue("status", RECEIPT_STATUS_GENERATED)
                        .addValue("generatedAt", now));
        return receiptId;
    }

    public String formatReceiptSummary(UUID businessId, UUID bookingId, UUID receiptId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("bookingId", bookingId)
                .addValue("receiptId", receiptId);
        var result = jdbcTemplate.query(
                """
                        select br.receipt_number, br.generated_at,
                               b.subject, c.display_name as customer_name,
                               bl.name as location_name, s.name as service_name
                        from booking_receipt br
                        join booking b on b.id = br.booking_id and b.business_id = br.business_id
                        join customer c on c.id = b.customer_id and c.business_id = b.business_id
                        left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
                        left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
                        where br.business_id = :businessId and br.id = :receiptId and br.booking_id = :bookingId
                        """,
                params,
                (rs, rowNum) -> new ReceiptSummary(
                        rs.getString("receipt_number"),
                        rs.getObject("generated_at", OffsetDateTime.class),
                        rs.getString("subject"),
                        rs.getString("customer_name"),
                        rs.getString("location_name"),
                        rs.getString("service_name")));
        if (result.isEmpty()) {
            return "Comprobante no disponible.";
        }
        ReceiptSummary r = result.getFirst();
        return "Comprobante " + r.receiptNumber() + " - " + r.serviceName()
                + " en " + r.locationName() + " para " + r.customerName()
                + " (generado " + r.generatedAt() + ")";
    }

    private record ReceiptSummary(String receiptNumber, OffsetDateTime generatedAt,
                                  String subject, String customerName,
                                  String locationName, String serviceName) {
    }
}
