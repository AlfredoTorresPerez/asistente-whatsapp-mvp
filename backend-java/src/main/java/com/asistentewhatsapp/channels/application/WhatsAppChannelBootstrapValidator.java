package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Valida al iniciar la aplicacion que el proveedor WhatsApp configurado
 * coincida con un canal realmente disponible. Una configuracion que haga
 * referencia a un proveedor eliminado o no habilitado detiene el arranque con
 * un mensaje claro, en lugar de fallar silenciosamente al despachar mensajes.
 */
@Component
public class WhatsAppChannelBootstrapValidator {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppChannelBootstrapValidator.class);

	private final WhatsAppChannelProperties properties;
	private final List<CanalWhatsApp> whatsAppChannels;

	public WhatsAppChannelBootstrapValidator(WhatsAppChannelProperties properties,
			List<CanalWhatsApp> whatsAppChannels) {
		this.properties = properties;
		this.whatsAppChannels = whatsAppChannels;
	}

	@PostConstruct
	void validate() {
		WhatsAppChannelProperties.Provider configured = properties.getProvider();
		WhatsAppChannelProvider expected = switch (configured) {
			case META_CLOUD_API -> WhatsAppChannelProvider.META_CLOUD_API;
			case SIMULATED -> WhatsAppChannelProvider.SIMULATED;
		};

		boolean available = whatsAppChannels.stream().anyMatch(channel -> channel.provider() == expected);
		if (!available) {
			throw new IllegalStateException("Proveedor WhatsApp configurado (" + configured + ") no esta disponible: "
					+ "habilite app.channels.whatsapp-cloud-api.enabled para META_CLOUD_API, "
					+ "o use SIMULATED para el modo simulado local.");
		}

		LOG.info("WhatsApp channel provider activo: {}", configured);
	}
}
