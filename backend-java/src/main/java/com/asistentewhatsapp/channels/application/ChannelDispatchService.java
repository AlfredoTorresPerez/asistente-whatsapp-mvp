package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.MessagingChannel;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.shared.exception.UnsupportedMessagingChannelException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChannelDispatchService {

	private final Map<MessageChannelType, MessagingChannel> channels;
	private final Map<WhatsAppChannelProvider, CanalWhatsApp> whatsAppChannels;
	private final WhatsAppChannelProperties whatsAppChannelProperties;

	public ChannelDispatchService(List<MessagingChannel> channels, List<CanalWhatsApp> whatsAppChannels,
			WhatsAppChannelProperties whatsAppChannelProperties) {
		this.channels = new EnumMap<>(MessageChannelType.class);
		for (MessagingChannel channel : channels) {
			this.channels.put(channel.type(), channel);
		}
		this.whatsAppChannels = whatsAppChannels.stream()
				.collect(Collectors.toMap(CanalWhatsApp::provider, Function.identity(), (left, right) -> left));
		this.whatsAppChannelProperties = whatsAppChannelProperties;
	}

	public ChannelDispatchResponse dispatch(ChannelDispatchRequest request) {
		MessagingChannel channel = resolveChannel(request.channelType());
		if (channel == null) {
			throw new UnsupportedMessagingChannelException(
					"El canal solicitado no esta configurado para este entorno.");
		}

		ChannelDelivery delivery = channel
				.send(new OutboundMessage(request.businessId(), request.recipientPhone(), request.body()));

		return new ChannelDispatchResponse(delivery.channelType(), delivery.externalMessageId(), delivery.status(),
				delivery.acceptedAt());
	}

	private MessagingChannel resolveChannel(MessageChannelType channelType) {
		if (channelType == MessageChannelType.WHATSAPP) {
			return resolveWhatsAppChannel();
		}
		return channels.get(channelType);
	}

	private CanalWhatsApp resolveWhatsAppChannel() {
		WhatsAppChannelProperties.Provider provider = whatsAppChannelProperties.getProvider();

		WhatsAppChannelProvider domainProvider = switch (provider) {
			case META_CLOUD_API -> WhatsAppChannelProvider.META_CLOUD_API;
			case SIMULATED -> WhatsAppChannelProvider.SIMULATED;
		};

		CanalWhatsApp channel = whatsAppChannels.get(domainProvider);
		if (channel == null) {
			throw new UnsupportedMessagingChannelException(
					"El proveedor WhatsApp configurado no esta habilitado: " + provider + ".");
		}
		return channel;
	}
}
