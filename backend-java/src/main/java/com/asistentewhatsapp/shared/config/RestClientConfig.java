package com.asistentewhatsapp.shared.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient.Builder restClientBuilder() {
		return RestClient.builder()
				.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
						.withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(10))));
	}
}
