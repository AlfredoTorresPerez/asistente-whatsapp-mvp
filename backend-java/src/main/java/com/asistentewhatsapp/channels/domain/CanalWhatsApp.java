package com.asistentewhatsapp.channels.domain;

/**
 * Puerto principal para desacoplar el dominio de la tecnologia concreta de
 * WhatsApp.
 *
 * <p>
 * Implementaciones: WhatsAppCloudApiAdapter (WhatsApp Cloud API de Meta, modo
 * productivo) y SimulatedWhatsAppProvider (modo simulado local, sin red ni
 * credenciales).
 */
public interface CanalWhatsApp extends MessagingChannel {

	WhatsAppChannelProvider provider();

	WhatsAppSessionStatus getStatus();

	WhatsAppSessionAction connect();

	WhatsAppSessionAction refreshQr();

	WhatsAppSessionAction disconnect();

	@Override
	default MessageChannelType type() {
		return MessageChannelType.WHATSAPP;
	}
}
