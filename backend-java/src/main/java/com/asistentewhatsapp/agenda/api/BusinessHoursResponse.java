package com.asistentewhatsapp.agenda.api;

import java.time.LocalTime;

public record BusinessHoursResponse(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
}