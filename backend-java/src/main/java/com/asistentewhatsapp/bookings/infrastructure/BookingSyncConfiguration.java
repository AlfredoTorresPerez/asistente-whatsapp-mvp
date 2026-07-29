package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.application.BookingPhoneObfuscator;
import com.asistentewhatsapp.bookings.application.BookingSyncProperties;
import com.asistentewhatsapp.bookings.application.SincronizadorReservaCompuesto;
import com.asistentewhatsapp.bookings.application.SincronizadorReservaEventos;
import com.asistentewhatsapp.bookings.application.SincronizadorReservaLocal;
import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(BookingSyncProperties.class)
public class BookingSyncConfiguration {

	@Bean
	public SincronizadorReservaLocal sincronizadorReservaLocal(BookingSyncJdbcRepository repository,
			BookingPhoneObfuscator phoneObfuscator) {
		return new SincronizadorReservaLocal(repository, phoneObfuscator);
	}

	@Bean
	public SincronizadorReservaEventos sincronizadorReservaEventos(BookingSyncEventJdbcRepository eventRepository,
			BookingSyncProperties properties) {
		return new SincronizadorReservaEventos(eventRepository, properties);
	}

	@Bean
	@Primary
	public SincronizadorReservaMotorReglas sincronizadorReservaCompuesto(SincronizadorReservaLocal local,
			SincronizadorReservaEventos eventos) {
		return new SincronizadorReservaCompuesto(local, eventos);
	}
}
