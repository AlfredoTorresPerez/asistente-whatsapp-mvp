package com.asistentewhatsapp.channels.domain;

/**
 * Puerto principal para desacoplar el dominio de la tecnologia concreta de
 * WhatsApp.
 *
 * <p>
 * Ambiente local: implementacion WhatsAppWebAdapter con whatsapp-web.js.
 * Ambiente productivo: implementacion WhatsAppCloudApiAdapter, habilitable por
 * configuracion.
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
