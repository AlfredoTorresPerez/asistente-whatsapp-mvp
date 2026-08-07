package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * FASE 06 — Fechas relativas y zona horaria.
 *
 * Verifica la resolución determinista de fechas relativas con fecha base y zona
 * horaria inyectables. Base por defecto de los catálogos: 2026-08-06 (jueves),
 * zona America/Santiago.
 */
class RelativeDateResolutionTest {

	private static final ZoneId ZONE = ZoneId.of("America/Santiago");
	private static final LocalDate BASE_THURSDAY = LocalDate.of(2026, 8, 6);

	private java.util.Optional<LocalDate> resolve(String phrase) {
		return resolve(phrase, BASE_THURSDAY);
	}

	private java.util.Optional<LocalDate> resolve(String phrase, LocalDate base) {
		return TransactionalAgendaBookingService.resolveDateAt(phrase, ZONE, base);
	}

	@Test
	void hoySeResuelveALaFechaBase() {
		assertThat(resolve("Quiero reservar hoy")).contains(LocalDate.of(2026, 8, 6));
	}

	@Test
	void mananaSeResuelveUnDiaDespues() {
		assertThat(resolve("para mañana")).contains(LocalDate.of(2026, 8, 7));
	}

	@Test
	void pasadoMananaNoSeReduceAManana() {
		assertThat(resolve("pasado mañana")).contains(LocalDate.of(2026, 8, 8));
	}

	@Test
	void esteViernesResuelveAlViernesDeLaSemanaActual() {
		assertThat(resolve("este viernes")).contains(LocalDate.of(2026, 8, 7));
	}

	@Test
	void proximoViernesResuelveAlViernesDeLaSemanaSiguiente() {
		assertThat(resolve("próximo viernes")).contains(LocalDate.of(2026, 8, 14));
	}

	@Test
	void proximoLunesResuelveAlLunesDeLaSemanaSiguiente() {
		assertThat(resolve("próximo lunes")).contains(LocalDate.of(2026, 8, 10));
	}

	@Test
	void finDeSemanaEsAmbiguedadExplicita() {
		assertThat(resolve("para el fin de semana")).isEmpty();
	}

	@Test
	void estaSemanaEsAmbiguedadExplicita() {
		assertThat(resolve("esta semana")).isEmpty();
	}

	@Test
	void proximaSemanaEsAmbiguedadExplicita() {
		assertThat(resolve("la próxima semana")).isEmpty();
	}

	@Test
	void enDosDiasSeResuelveDosDiasDespues() {
		assertThat(resolve("en dos días")).contains(LocalDate.of(2026, 8, 8));
	}

	@Test
	void dentroDeUnaSemanaSeResuelveSieteDiasDespues() {
		assertThat(resolve("dentro de una semana")).contains(LocalDate.of(2026, 8, 13));
	}

	@Test
	void fechaAbsolutaValidaSeResuelveTalCual() {
		assertThat(resolve("15/08/2026")).contains(LocalDate.of(2026, 8, 15));
		assertThat(resolve("2026-08-15")).contains(LocalDate.of(2026, 8, 15));
	}

	@Test
	void fechaInexistenteQuedaComoAmbiguedad() {
		assertThat(resolve("31/02/2026")).isEmpty();
		assertThat(resolve("2026-02-31")).isEmpty();
	}

	@Test
	void fechaPasadaSeResuelveComoFechaCanonicaYElOrigenLaRechazaAparte() {
		assertThat(resolve("15/07/2026")).contains(LocalDate.of(2026, 7, 15));
	}

	@Test
	void fraseAmbiguasSinDiaBaseQuedaVacias() {
		assertThat(resolve("un día de estos")).isEmpty();
		assertThat(resolve("cuando pueda")).isEmpty();
	}

	@Test
	void esteSabadoTrasJuevesResuelveSiguienteDiaDeLaSemana() {
		assertThat(resolve("este sábado")).contains(LocalDate.of(2026, 8, 8));
	}

	@Test
	void esteSabadoElSabadoEsHoyYNoseEmpuja() {
		LocalDate saturday = LocalDate.of(2026, 8, 8);
		assertThat(TransactionalAgendaBookingService.resolveDateAt("este sábado", ZONE, saturday)).contains(saturday);
	}

	@Test
	void esteLunesDichoUnSabadoSeEmpujaALaLunesSiguiente() {
		LocalDate saturday = LocalDate.of(2026, 8, 8);
		assertThat(TransactionalAgendaBookingService.resolveDateAt("este lunes", ZONE, saturday))
				.contains(LocalDate.of(2026, 8, 10));
	}

	@Test
	void cambioDeMesSeManejaConPlusDias() {
		LocalDate base = LocalDate.of(2026, 8, 31);
		assertThat(TransactionalAgendaBookingService.resolveDateAt("mañana", ZONE, base))
				.contains(LocalDate.of(2026, 9, 1));
	}

	@Test
	void cambioDeAnhoSeManejaConPlusDias() {
		LocalDate base = LocalDate.of(2026, 12, 31);
		assertThat(TransactionalAgendaBookingService.resolveDateAt("mañana", ZONE, base))
				.contains(LocalDate.of(2027, 1, 1));
	}

	@Test
	void anhoBisiestoAcepta29DeFebrero() {
		assertThat(resolve("29/02/2028")).contains(LocalDate.of(2028, 2, 29));
		assertThat(resolve("2028-02-29")).contains(LocalDate.of(2028, 2, 29));
	}

	@Test
	void anhoNoBisiestoRechaza29DeFebrero() {
		assertThat(resolve("29/02/2026")).isEmpty();
	}

	@Test
	void zonaHorariaNoAlteraLaAritmeticaDeFecha() {
		ZoneId santiago = ZoneId.of("America/Santiago");
		ZoneId otros = ZoneId.of("America/New_York");
		LocalDate base = LocalDate.of(2026, 9, 5);
		assertThat(TransactionalAgendaBookingService.resolveDateAt("mañana", santiago, base))
				.isEqualTo(TransactionalAgendaBookingService.resolveDateAt("mañana", otros, base));
	}
}