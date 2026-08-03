package com.asistentewhatsapp.customers.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.asistentewhatsapp.customers.api.CustomerSearchResponse;
import com.asistentewhatsapp.customers.infrastructure.CustomerSearchJdbcRepository;
import com.asistentewhatsapp.shared.util.PhoneUtils;

@Service
public class CustomerSearchService {

	private final CustomerSearchJdbcRepository customerRepository;

	public CustomerSearchService(CustomerSearchJdbcRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public List<CustomerSearchResponse> search(UUID businessId, String phone, String name) {
		return search(businessId, phone, name, null);
	}

	public List<CustomerSearchResponse> search(UUID businessId, String phone, String name, String email) {
		if (phone != null && !phone.isBlank()) {
			String normalized = PhoneUtils.normalizeChilePhone(phone);
			return customerRepository.findByPhone(businessId,
					normalized != null ? normalized : phone.replaceAll("\\D", ""));
		}
		if (email != null && !email.isBlank()) {
			return customerRepository.findByEmail(businessId, email.trim());
		}
		if (name != null && !name.isBlank()) {
			return customerRepository.findByName(businessId, name.trim());
		}
		return List.of();
	}
}
