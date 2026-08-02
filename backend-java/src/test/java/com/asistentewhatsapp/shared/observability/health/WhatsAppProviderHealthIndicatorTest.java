package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.channels.application.WhatsAppChannelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class WhatsAppProviderHealthIndicatorTest {

	@Test
	void reportsSimulatedProvider() {
		WhatsAppChannelProperties properties = mock(WhatsAppChannelProperties.class);
		when(properties.getProvider()).thenReturn(WhatsAppChannelProperties.Provider.SIMULATED);
		WhatsAppProviderHealthIndicator indicator = new WhatsAppProviderHealthIndicator(properties);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("proveedor", "SIMULATED").containsEntry("canal", "simulado");
	}

	@Test
	void reportsRealProvider() {
		WhatsAppChannelProperties properties = mock(WhatsAppChannelProperties.class);
		when(properties.getProvider()).thenReturn(WhatsAppChannelProperties.Provider.META_CLOUD_API);
		WhatsAppProviderHealthIndicator indicator = new WhatsAppProviderHealthIndicator(properties);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("proveedor", "META_CLOUD_API").containsEntry("canal", "real");
	}
}
