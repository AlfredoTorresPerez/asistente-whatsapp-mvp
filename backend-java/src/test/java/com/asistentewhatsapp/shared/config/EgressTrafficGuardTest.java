package com.asistentewhatsapp.shared.config;

import com.asistentewhatsapp.shared.config.EgressTrafficGuard.EgressTrafficBlockedException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EgressTrafficGuardTest {

	private final EgressTrafficGuard guard = new EgressTrafficGuard();

	private ClientHttpResponse execute(String url) throws Exception {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create(url));
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse response = new MockClientHttpResponse(new byte[0], 200);
		when(execution.execute(any(), any())).thenReturn(response);
		return guard.intercept(request, new byte[0], execution);
	}

	@Test
	void allowsLocalhost() throws Exception {
		assertThat(execute("http://localhost:8080/api/health").getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void allowsContainerServices() throws Exception {
		assertThat(execute("http://mailpit:8025/").getStatusCode().value()).isEqualTo(200);
		assertThat(execute("http://tempo:4318/v1/traces").getStatusCode().value()).isEqualTo(200);
		assertThat(execute("http://postgres:5432/").getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void allowsDotLocalHosts() throws Exception {
		assertThat(execute("http://api.local:3000/").getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void blocksGraphFacebook() {
		assertThatThrownBy(() -> execute("https://graph.facebook.com/v23.0/messages"))
				.isInstanceOf(EgressTrafficBlockedException.class).hasMessageContaining("graph.facebook.com");
	}

	@Test
	void blocksOpenAi() {
		assertThatThrownBy(() -> execute("https://api.openai.com/v1/responses"))
				.isInstanceOf(EgressTrafficBlockedException.class).hasMessageContaining("api.openai.com");
	}

	@Test
	void blocksGmailSmtp() {
		assertThatThrownBy(() -> execute("https://smtp.gmail.com:587/"))
				.isInstanceOf(EgressTrafficBlockedException.class).hasMessageContaining("smtp.gmail.com");
	}

	@Test
	void blocksMercadoPago() {
		assertThatThrownBy(() -> execute("https://api.mercadopago.com/v1/payments"))
				.isInstanceOf(EgressTrafficBlockedException.class).hasMessageContaining("api.mercadopago.com");
	}
}
