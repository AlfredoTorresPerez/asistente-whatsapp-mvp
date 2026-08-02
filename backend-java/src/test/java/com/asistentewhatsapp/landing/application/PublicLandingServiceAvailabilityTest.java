package com.asistentewhatsapp.landing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicLandingServiceAvailabilityTest {

	private static final UUID LOCATION_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
	private static final UUID SERVICE_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
	private static final UUID PROFESSIONAL_A = UUID.fromString("83000000-0000-0000-0000-000000000001");
	private static final UUID PROFESSIONAL_B = UUID.fromString("83000000-0000-0000-0000-000000000002");
	private static final UUID ROOM_ONE = UUID.fromString("84000000-0000-0000-0000-000000000001");
	private static final UUID ROOM_TWO = UUID.fromString("84000000-0000-0000-0000-000000000002");

	private AgendaSlotResponse slot(String start, String end, UUID professionalId, String professionalName, UUID roomId,
			String roomName) {
		return new AgendaSlotResponse(OffsetDateTime.parse(start), OffsetDateTime.parse(end), LOCATION_ID,
				"Sucursal Centro", SERVICE_ID, "Limpieza facial", 45, professionalId, professionalName, roomId,
				roomName, true, "Disponible");
	}

	private List<OffsetDateTime> startTimes(List<AgendaSlotResponse> slots) {
		return slots.stream().map(AgendaSlotResponse::startsAt).toList();
	}

	@Test
	void sortsOutOfOrderSlotsAscendingByStartTime() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T10:00:00-04:00", "2026-08-05T10:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:45:00-04:00", "2026-08-05T10:30:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T12:30:00-04:00", "2026-08-05T13:15:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null)),
				40);

		assertThat(startTimes(result)).containsExactly(OffsetDateTime.parse("2026-08-05T09:00:00-04:00"),
				OffsetDateTime.parse("2026-08-05T09:45:00-04:00"), OffsetDateTime.parse("2026-08-05T10:00:00-04:00"),
				OffsetDateTime.parse("2026-08-05T12:30:00-04:00"));
	}

	@Test
	void comparesQuarterToTenCorrectlyAcrossNoonBoundary() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T10:00:00-04:00", "2026-08-05T10:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:45:00-04:00", "2026-08-05T10:30:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null)),
				40);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).startsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T09:45:00-04:00"));
		assertThat(result.get(1).startsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T10:00:00-04:00"));
	}

	@Test
	void preservesDifferentProfessionalsAtTheSameStartTime() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_B, "Ana Profesional", null,
						null)),
				40);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(AgendaSlotResponse::professionalId).containsExactlyInAnyOrder(PROFESSIONAL_A,
				PROFESSIONAL_B);
	}

	@Test
	void preservesDifferentRoomsAtTheSameStartTime() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", ROOM_ONE,
						"Cabina 1"),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", ROOM_TWO,
						"Cabina 2")),
				40);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(AgendaSlotResponse::roomId).containsExactlyInAnyOrder(ROOM_ONE, ROOM_TWO);
	}

	@Test
	void removesExactDuplicatesOnly() {
		AgendaSlotResponse original = slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A,
				"Carla Mendez", ROOM_ONE, "Cabina 1");
		AgendaSlotResponse exactCopy = slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A,
				"Carla Mendez", ROOM_ONE, "Cabina 1");

		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(original, exactCopy), 40);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).startsAt()).isEqualTo(original.startsAt());
	}

	@Test
	void doesNotEliminateLegitimateSimultaneousAvailability() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_B, "Ana Profesional", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", ROOM_TWO,
						"Cabina 2")),
				40);

		assertThat(result).hasSize(3);
	}

	@Test
	void breaksTiesByProfessionalNameThenRoomName() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_B, "Ana Profesional", null,
						null)),
				40);

		assertThat(result.get(0).professionalName()).isEqualTo("Ana Profesional");
		assertThat(result.get(1).professionalName()).isEqualTo("Carla Mendez");
	}

	@Test
	void returnsEmptyListForNoSlots() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(), 40);

		assertThat(result).isEmpty();
	}

	@Test
	void keepsStartAndEndTimesUnchanged() {
		AgendaSlotResponse input = slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A,
				"Carla Mendez", ROOM_ONE, "Cabina 1");

		AgendaSlotResponse result = PublicLandingService.normalizeAndSortSlots(List.of(input), 40).getFirst();

		assertThat(result.startsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T09:00:00-04:00"));
		assertThat(result.endsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T09:45:00-04:00"));
		assertThat(result.durationMinutes()).isEqualTo(45);
		assertThat(result.available()).isTrue();
	}

	@Test
	void doesNotInventNorDropAvailabilityBeyondExactDuplicates() {
		AgendaSlotResponse a = slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A,
				"Carla Mendez", null, null);
		AgendaSlotResponse b = slot("2026-08-05T12:00:00-04:00", "2026-08-05T12:45:00-04:00", PROFESSIONAL_A,
				"Carla Mendez", null, null);

		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(a, b), 40);

		assertThat(result).hasSize(2);
		assertThat(result).containsExactly(a, b);
	}

	@Test
	void truncatesToLimitAfterSorting() {
		List<AgendaSlotResponse> result = PublicLandingService.normalizeAndSortSlots(List.of(
				slot("2026-08-05T12:00:00-04:00", "2026-08-05T12:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T09:00:00-04:00", "2026-08-05T09:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null),
				slot("2026-08-05T10:00:00-04:00", "2026-08-05T10:45:00-04:00", PROFESSIONAL_A, "Carla Mendez", null,
						null)),
				2);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).startsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T09:00:00-04:00"));
		assertThat(result.get(1).startsAt()).isEqualTo(OffsetDateTime.parse("2026-08-05T10:00:00-04:00"));
	}
}
