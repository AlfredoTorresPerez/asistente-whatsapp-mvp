package com.asistentewhatsapp.orders.api;

import com.asistentewhatsapp.orders.application.OrderService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
	@GetMapping({"/api/orders", "/api/v1/orders"})
	public PagedResponse<OrderSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String search, @RequestParam(required = false) String status,
			@RequestParam(required = false) String paymentStatus) {
		return orderService.list(authenticatedUser, page, size, search, status, paymentStatus);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping(value = {"/api/orders", "/api/v1/orders"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse create(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CreateOrderRequest request) {
		return orderService.create(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping(value = {"/api/orders/from-conversation/{conversationId}",
			"/api/v1/orders/from-conversation/{conversationId}", "/api/conversations/{conversationId}/orders",
			"/api/v1/conversations/{conversationId}/orders"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse createFromConversation(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID conversationId, @Valid @RequestBody CreateOrderRequest request) {
		return orderService.createFromConversation(authenticatedUser, conversationId, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping(value = {"/api/orders/from-prospect/{prospectId}", "/api/v1/orders/from-prospect/{prospectId}",
			"/api/prospects/{prospectId}/orders",
			"/api/v1/prospects/{prospectId}/orders"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse createFromProspect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID prospectId, @Valid @RequestBody CreateOrderRequest request) {
		return orderService.createFromLead(authenticatedUser, prospectId, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
	@GetMapping({"/api/orders/{id}", "/api/v1/orders/{id}"})
	public OrderDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id) {
		return orderService.getDetail(authenticatedUser, id);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PutMapping(value = {"/api/orders/{id}", "/api/v1/orders/{id}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse update(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody UpdateOrderRequest request) {
		return orderService.update(authenticatedUser, id, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping(value = {"/api/orders/{id}/items",
			"/api/v1/orders/{id}/items"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse addProducts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody List<CreateOrderItemRequest> items) {
		return orderService.addProducts(authenticatedUser, id, items);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PatchMapping(value = {"/api/orders/{id}/status",
			"/api/v1/orders/{id}/status"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse updateStatus(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request) {
		return orderService.updateStatus(authenticatedUser, id, request.status());
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping(value = {"/api/orders/{id}/payment",
			"/api/v1/orders/{id}/payment"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OrderDetailResponse registerPayment(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody RegisterPaymentRequest request) {
		return orderService.registerPayment(authenticatedUser, id, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ORDER_MANAGE')")
	@PostMapping({"/api/orders/{id}/send-summary", "/api/v1/orders/{id}/send-summary"})
	public SendOrderSummaryResponse sendSummary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id) {
		return orderService.sendSummary(authenticatedUser, id);
	}
}
