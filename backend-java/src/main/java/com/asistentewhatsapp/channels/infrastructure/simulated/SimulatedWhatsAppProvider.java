package com.asistentewhatsapp.channels.infrastructure.simulated;

import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionAction;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Proveedor simulado de WhatsApp: entrega mensajes en memoria, sin red, sin
 * credenciales y sin dependencias externas. Es el proveedor por defecto en
 * ambientes locales de desarrollo y demos.
 */
@Component
public class SimulatedWhatsAppProvider implements CanalWhatsApp {

	private static final Logger LOG = LoggerFactory.getLogger(SimulatedWhatsAppProvider.class);
	private static final int MAX_RECENT_DELIVERIES = 100;

	private final Deque<SimulatedDelivery> recentDeliveries = new ArrayDeque<>();

	@Override
	public WhatsAppChannelProvider provider() {
		return WhatsAppChannelProvider.SIMULATED;
	}

	@Override
	public WhatsAppSessionStatus getStatus() {
		return new WhatsAppSessionStatus("simulated", "CONNECTED", "SIMULADO", null, "SIMULATED_LOCAL",
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Override
	public WhatsAppSessionAction connect() {
		return new WhatsAppSessionAction("simulated", "CONNECTED", "SIMULADO", null,
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Override
	public WhatsAppSessionAction refreshQr() {
		return connect();
	}

	@Override
	public WhatsAppSessionAction disconnect() {
		return new WhatsAppSessionAction("simulated", "DISCONNECTED", "SIMULADO", null,
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Override
	public ChannelDelivery send(OutboundMessage outboundMessage) {
		String messageId = "sim-" + UUID.randomUUID();
		Instant acceptedAt = Instant.now();
		synchronized (recentDeliveries) {
			recentDeliveries.addFirst(new SimulatedDelivery(outboundMessage.businessId(),
					outboundMessage.recipientPhone(), outboundMessage.body(), messageId, acceptedAt));
			while (recentDeliveries.size() > MAX_RECENT_DELIVERIES) {
				recentDeliveries.removeLast();
			}
		}
		LOG.info("Simulated WhatsApp message delivered: id={} to={}", messageId, outboundMessage.recipientPhone());
		return new ChannelDelivery(MessageChannelType.WHATSAPP, messageId, "SIMULATED", acceptedAt);
	}

	public List<SimulatedDelivery> getRecentDeliveries() {
		synchronized (recentDeliveries) {
			return List.copyOf(recentDeliveries);
		}
	}

	public record SimulatedDelivery(UUID businessId, String recipientPhone, String body, String externalMessageId,
			Instant acceptedAt) {
	}
}
