package com.asistentewhatsapp.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogSanitizerTest {

	@AfterEach
	void reset() {
		LogSanitizer.setIncludeMessageBody(false);
	}

	@Test
	void messageSummaryOmitsBodyByDefault() {
		String summary = LogSanitizer.messageSummary("message",
				"Hola Ana, confirma en http://localhost/reservas/confirmar/abc");

		assertThat(summary).contains("messageLength=").contains("messageContainsLink=true").doesNotContain("Hola Ana")
				.doesNotContain("/abc");
	}

	@Test
	void masksPhoneExternalIdAndSensitiveMapValues() {
		String summary = LogSanitizer.summarizeMap(Map.of("telefono", "+56912345678", "responseText",
				"Respuesta larga con link http://localhost/reservas/confirmar/token-real"));

		assertThat(LogSanitizer.maskPhone("+56912345678")).isEqualTo("+569****5678");
		assertThat(LogSanitizer.maskExternalId("wamid.HBgLMTIzNDU2")).isEqualTo("wami...NDU2");
		assertThat(summary).contains("responseText=length=").doesNotContain("token-real")
				.doesNotContain("+56912345678");
	}

	@Test
	void clientErrorSummaryOmitsStackBodyByDefault() {
		String summary = LogSanitizer.clientErrorSummary("Error al renderizar", "stack detallado\nen linea 42");

		assertThat(summary).contains("Error al renderizar").contains("stackLength=27").doesNotContain("linea 42");
	}

	@Test
	void clientErrorSummaryIncludesStackBodyWhenEnabled() {
		LogSanitizer.setIncludeMessageBody(true);
		String summary = LogSanitizer.clientErrorSummary("Error al renderizar", "stack detallado en linea 42");

		assertThat(summary).contains("Error al renderizar").contains("stack=").contains("linea 42");
	}

	@Test
	void clientErrorSummaryHandlesNullStackAndRedactsPhones() {
		assertThat(LogSanitizer.clientErrorSummary("Fallo", null)).isEqualTo("Fallo");
		assertThat(LogSanitizer.clientErrorSummary("Error con +56912345678", null))
				.isEqualTo("Error con [TELEFONO_REDACTADO]");
	}
}
