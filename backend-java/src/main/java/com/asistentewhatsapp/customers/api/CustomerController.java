package com.asistentewhatsapp.customers.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asistentewhatsapp.customers.application.CustomerSearchService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerController {

	private final CustomerSearchService customerSearchService;

	public CustomerController(CustomerSearchService customerSearchService) {
		this.customerSearchService = customerSearchService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AGENDA_VIEW')")
	@GetMapping("/customers/search")
	public List<CustomerSearchResponse> searchCustomers(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(required = false) String phone, @RequestParam(required = false) String name,
			@RequestParam(required = false) String email, @RequestParam(required = false) UUID locationId) {
		return customerSearchService.search(authenticatedUser.businessId(), phone, name, email);
	}
}
