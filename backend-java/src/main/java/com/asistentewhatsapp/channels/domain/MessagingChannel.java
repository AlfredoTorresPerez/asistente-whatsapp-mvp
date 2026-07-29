package com.asistentewhatsapp.channels.domain;

public interface MessagingChannel {

	MessageChannelType type();

	ChannelDelivery send(OutboundMessage outboundMessage);
}
