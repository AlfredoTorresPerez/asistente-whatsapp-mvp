package com.asistentewhatsapp.landing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicCenterServiceWhatsAppTest {

	@Test
	void prefersChannelPhoneNumberWhenPresent() {
		String resolved = PublicCenterService.resolveWhatsAppPhone("+56927305158", "+56911111111", "+56 9 2730 5158",
				"+56955550100");
		assertThat(resolved).isEqualTo("+56927305158");
	}

	@Test
	void fallsBackToBusinessPhoneWhenChannelPhoneIsBlank() {
		String resolved = PublicCenterService.resolveWhatsAppPhone("  ", "+56927305158", "+56 9 2730 5158",
				"+56955550100");
		assertThat(resolved).isEqualTo("+56927305158");
	}

	@Test
	void fallsBackToBusinessPhoneWhenChannelPhoneIsNull() {
		String resolved = PublicCenterService.resolveWhatsAppPhone(null, "+56927305158", null, "+56955550100");
		assertThat(resolved).isEqualTo("+56927305158");
	}

	@Test
	void fallsBackToChannelDisplayWhenBusinessPhoneIsMissing() {
		String resolved = PublicCenterService.resolveWhatsAppPhone(null, null, "+56 9 2730 5158", "+56955550100");
		assertThat(resolved).isEqualTo("+56 9 2730 5158");
	}

	@Test
	void usesLocationWhatsappAsLastResort() {
		String resolved = PublicCenterService.resolveWhatsAppPhone(null, null, null, "+56955550100");
		assertThat(resolved).isEqualTo("+56955550100");
	}

	@Test
	void returnsNullWhenEverythingIsBlank() {
		String resolved = PublicCenterService.resolveWhatsAppPhone(null, "   ", null, "");
		assertThat(resolved).isNull();
	}

	@Test
	void resolvesTheDemoLocationScenarioToBusinessNumber() {
		// Canal sin phone_number (solo display), empresa con support_phone:
		// antes resolvia al whatsapp de la sede (numero demo) -> ahora usa la empresa
		String resolved = PublicCenterService.resolveWhatsAppPhone(null, "+56927305158", "+56 9 2730 5158",
				"+56955550100");
		assertThat(resolved).isEqualTo("+56927305158");
	}
}
