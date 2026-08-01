package com.asistentewhatsapp.channels.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.whatsapp")
public class WhatsAppChannelProperties {

	private Provider provider = Provider.SIMULATED;

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public enum Provider {
		META_CLOUD_API, SIMULATED
	}
}
