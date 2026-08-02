package com.asistentewhatsapp.agenda.application;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Tramo horario de una disponibilidad de agenda, clasificado exclusivamente por
 * su hora de inicio: antes de las 12:00 es manana; desde las 12:00 es tarde.
 */
public enum SlotTimePeriod {
	MORNING, AFTERNOON;

	/** Limite centralizado entre manana y tarde. */
	public static final LocalTime NOON = LocalTime.NOON;

	public static SlotTimePeriod of(LocalTime startTime) {
		return startTime.isBefore(NOON) ? MORNING : AFTERNOON;
	}

	public static SlotTimePeriod of(OffsetDateTime startsAt) {
		return of(startsAt.toLocalTime());
	}
}
