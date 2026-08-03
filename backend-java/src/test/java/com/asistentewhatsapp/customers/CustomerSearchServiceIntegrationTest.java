package com.asistentewhatsapp.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.customers.api.CustomerSearchResponse;
import com.asistentewhatsapp.customers.application.CustomerSearchService;
import com.asistentewhatsapp.customers.infrastructure.CustomerSearchJdbcRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomerSearchServiceIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CustomerSearchJdbcRepository repository;

	@Autowired
	private CustomerSearchService service;

	private static final UUID BUSINESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");

	@BeforeEach
	void setUp() {
		jdbcTemplate.update(
				"""
						insert into business (id, code, company_name, business_name, timezone, currency, contact_email, support_phone, address)
						values (?, 'CUST-TEST', 'Test Business', 'Test Business', 'America/Santiago', 'CLP',
								'test@business.cl', '+56900000000', 'Test Address')
						on conflict (id) do nothing
						""",
				BUSINESS_ID);

		jdbcTemplate.update(
				"""
						insert into customer (id, business_id, first_name, last_name, display_name, phone, normalized_phone, email, active)
						values
						  (?, ?, 'Maria', 'Perez', 'Maria Perez', '56912345678', '+56912345678', 'maria@test.cl', true),
						  (?, ?, 'Juan', 'Perez', 'Juan Perez', '56987654321', '+56987654321', 'juan@test.cl', true),
						  (?, ?, 'Carlos', 'Santos', 'Carlos Santos', '56911111111', '+56911111111', null, true)
						on conflict (id) do nothing
						""",
				UUID.fromString("11111111-1111-4111-a111-111111111111"), BUSINESS_ID,
				UUID.fromString("22222222-2222-4222-a222-222222222222"), BUSINESS_ID,
				UUID.fromString("33333333-3333-4333-a333-333333333333"), BUSINESS_ID);
	}

	@Test
	void findsCustomerByPhone() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, "56912345678", null);
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().displayName()).isEqualTo("Maria Perez");
	}

	@Test
	void findsCustomerByPhoneWithPlusPrefix() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, "+56912345678", null);
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().displayName()).isEqualTo("Maria Perez");
	}

	@Test
	void findsCustomerByPartialName() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, null, "Perez");
		assertThat(results).hasSize(2);
		assertThat(results.stream().map(CustomerSearchResponse::displayName)).containsExactlyInAnyOrder("Maria Perez",
				"Juan Perez");
	}

	@Test
	void findsCustomerByFirstName() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, null, "Juan");
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().displayName()).isEqualTo("Juan Perez");
	}

	@Test
	void returnsEmptyWhenNoMatch() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, "56999999999", null);
		assertThat(results).isEmpty();
	}

	@Test
	void returnsEmptyWhenBothParametersNull() {
		List<CustomerSearchResponse> results = service.search(BUSINESS_ID, null, null);
		assertThat(results).isEmpty();
	}
}
