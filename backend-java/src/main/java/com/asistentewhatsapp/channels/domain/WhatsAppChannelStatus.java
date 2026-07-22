package com.asistentewhatsapp.channels.domain;

public final class WhatsAppChannelStatus {

    private WhatsAppChannelStatus() {}

    public static final String REGISTRATION_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String REGISTRATION_PENDING = "PENDING";
    public static final String REGISTRATION_REGISTERED = "REGISTERED";
    public static final String REGISTRATION_ERROR = "ERROR";

    public static final String OPERATIONAL_INACTIVE = "INACTIVE";
    public static final String OPERATIONAL_CONFIGURING = "CONFIGURING";
    public static final String OPERATIONAL_CONNECTED = "CONNECTED";
    public static final String OPERATIONAL_DEGRADED = "DEGRADED";
    public static final String OPERATIONAL_DISCONNECTED = "DISCONNECTED";
    public static final String OPERATIONAL_ERROR = "ERROR";

    public static final String WEBHOOK_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String WEBHOOK_PENDING_VALIDATION = "PENDING_VALIDATION";
    public static final String WEBHOOK_VERIFIED = "VERIFIED";
    public static final String WEBHOOK_SUBSCRIBED = "SUBSCRIBED";
    public static final String WEBHOOK_ERROR = "ERROR";

    public static final String CREDENTIAL_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String CREDENTIAL_CONFIGURED = "CONFIGURED";
    public static final String CREDENTIAL_EXPIRING = "EXPIRING";
    public static final String CREDENTIAL_EXPIRED = "EXPIRED";
    public static final String CREDENTIAL_INVALID = "INVALID";

    public static final String PROVIDER_WHATSAPP_WEB = "WHATSAPP_WEB";
    public static final String PROVIDER_META_CLOUD_API = "META_CLOUD_API";
}
