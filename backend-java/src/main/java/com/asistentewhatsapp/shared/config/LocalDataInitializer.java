package com.asistentewhatsapp.shared.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(1000)
public class LocalDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(LocalDataInitializer.class);
	private static final String BUSINESS_ID = "11111111-1111-1111-1111-111111111111";

	private final JdbcTemplate jdbc;

	public LocalDataInitializer(DataSource dataSource) {
		this.jdbc = new JdbcTemplate(dataSource);
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Inicializando datos de semilla para perfil local...");
		updateStaleBookings();
		log.info("Datos de semilla local inicializados correctamente.");
	}

	private void updateStaleBookings() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		LocalDate dayAfter = LocalDate.now().plusDays(2);

		int facialBookingId = jdbc.queryForObject(
				"SELECT count(*) FROM booking WHERE id = ?::uuid AND business_id = ?::uuid", Integer.class,
				"68000000-0000-0000-0000-000000000001", BUSINESS_ID);

		int updated = jdbc.update(
				"""
						UPDATE booking SET
						    starts_at = CASE id
						        WHEN ?::uuid THEN ?::timestamp
						        WHEN ?::uuid THEN ?::timestamp
						    END,
						    ends_at = CASE id
						        WHEN ?::uuid THEN ?::timestamp + (duration_minutes || ' minutes')::interval
						        WHEN ?::uuid THEN ?::timestamp + (duration_minutes || ' minutes')::interval
						    END,
						    service_id = CASE id
						        WHEN ?::uuid THEN (SELECT id FROM aesthetic_service WHERE business_id = ?::uuid AND code = 'FAC-LIMPIEZA' LIMIT 1)
						        WHEN ?::uuid THEN (SELECT id FROM aesthetic_service WHERE business_id = ?::uuid AND code = 'DEP-LASER' LIMIT 1)
						    END,
						    professional_id = CASE id
						        WHEN ?::uuid THEN (SELECT id FROM aesthetic_professional WHERE business_id = ?::uuid AND full_name = 'Carla Mendez' LIMIT 1)
						        WHEN ?::uuid THEN (SELECT id FROM aesthetic_professional WHERE business_id = ?::uuid AND full_name = 'Daniela Soto' LIMIT 1)
						    END,
						    room_id = CASE id
						        WHEN ?::uuid THEN (SELECT r.id FROM agenda_room r WHERE r.business_id = ?::uuid AND r.code LIKE '%cabina-1' LIMIT 1)
						        WHEN ?::uuid THEN (SELECT r.id FROM agenda_room r WHERE r.business_id = ?::uuid AND r.code LIKE '%cabina-2' LIMIT 1)
						    END
						WHERE business_id = ?::uuid
						  AND id IN (?::uuid, ?::uuid)
						  AND starts_at < CURRENT_TIMESTAMP - INTERVAL '1 hour'
						""",
				"68000000-0000-0000-0000-000000000001", LocalDateTime.of(tomorrow, LocalTime.of(14, 0)),
				"68000000-0000-0000-0000-000000000002", LocalDateTime.of(tomorrow, LocalTime.of(17, 0)),
				"68000000-0000-0000-0000-000000000001", LocalDateTime.of(tomorrow, LocalTime.of(14, 0)),
				"68000000-0000-0000-0000-000000000002", LocalDateTime.of(tomorrow, LocalTime.of(17, 0)),
				"68000000-0000-0000-0000-000000000001", BUSINESS_ID, "68000000-0000-0000-0000-000000000002",
				BUSINESS_ID, "68000000-0000-0000-0000-000000000001", BUSINESS_ID,
				"68000000-0000-0000-0000-000000000002", BUSINESS_ID, "68000000-0000-0000-0000-000000000001",
				BUSINESS_ID, "68000000-0000-0000-0000-000000000002", BUSINESS_ID, BUSINESS_ID,
				"68000000-0000-0000-0000-000000000001", "68000000-0000-0000-0000-000000000002");

		if (updated > 0) {
			log.info("Se actualizaron {} bookings con fechas futuras y referencias a servicio/profesional/cabina.",
					updated);
		} else {
			log.info("No se requirio actualizar bookings (ya estan con fechas futuras).");
		}
	}
}
