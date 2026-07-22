package com.asistentewhatsapp.channels.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.whatsapp")
public class WhatsAppChannelProperties {

    private Provider provider = Provider.WEB;

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public enum Provider {
        WEB,
        WHATSAPP_WEB,
        CLOUD_API,
        META_CLOUD_API,
        DISABLED
    }
}
