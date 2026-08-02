package com.asistentewhatsapp.shared.observability.health;

import com.asistentewhatsapp.channels.application.WhatsAppChannelProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppProviderHealthIndicator implements HealthIndicator {

	private final WhatsAppChannelProperties properties;

	public WhatsAppProviderHealthIndicator(WhatsAppChannelProperties properties) {
		this.properties = properties;
	}

	@Override
	public Health health() {
		String provider = properties.getProvider().name();
		boolean simulated = properties.getProvider() == WhatsAppChannelProperties.Provider.SIMULATED;
		return Health.up().withDetail("proveedor", provider).withDetail("canal", simulated ? "simulado" : "real")
				.build();
	}
}
