package com.asistentewhatsapp.shared.observability;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.logging")
public class AppLoggingProperties {

	private boolean includeMessageBody = false;

	@PostConstruct
	void apply() {
		LogSanitizer.setIncludeMessageBody(includeMessageBody);
	}

	public boolean isIncludeMessageBody() {
		return includeMessageBody;
	}

	public void setIncludeMessageBody(boolean includeMessageBody) {
		this.includeMessageBody = includeMessageBody;
		LogSanitizer.setIncludeMessageBody(includeMessageBody);
	}
}
