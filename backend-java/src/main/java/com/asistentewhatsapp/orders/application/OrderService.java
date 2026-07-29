package com.asistentewhatsapp.orders.application;

import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.orders.api.CreateOrderItemRequest;
import com.asistentewhatsapp.orders.api.CreateOrderRequest;
import com.asistentewhatsapp.orders.api.OrderDetailResponse;
import com.asistentewhatsapp.orders.api.OrderSummaryResponse;
import com.asistentewhatsapp.orders.api.RegisterPaymentRequest;
import com.asistentewhatsapp.orders.api.SendOrderSummaryResponse;
import com.asistentewhatsapp.orders.api.UpdateOrderRequest;
import com.asistentewhatsapp.orders.infrastructure.OrderJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final Set<String> ORDER_STATUSES = Set.of("DRAFT", "CONFIRMED", "PREPARING", "READY", "DELIVERED",
			"CANCELLED");

	private final OrderJdbcRepository orderJdbcRepository;
	private final ChannelDispatchService channelDispatchService;

	public OrderService(OrderJdbcRepository orderJdbcRepository, ChannelDispatchService channelDispatchService) {
		this.orderJdbcRepository = orderJdbcRepository;
		this.channelDispatchService = channelDispatchService;
	}

	@Transactional(readOnly = true)
	public PagedResponse<OrderSummaryResponse> list(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String status, String paymentStatus) {
		int resolvedPage = Math.max(page, 0);
		int resolvedSize = Math.min(Math.max(size, 1), 100);
		return orderJdbcRepository.findOrders(authenticatedUser.businessId(), resolvedPage, resolvedSize, search,
				normalizeOptionalStatus(status), normalizeOptionalPaymentStatus(paymentStatus));
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getDetail(AuthenticatedUser authenticatedUser, UUID orderId) {
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional
	public OrderDetailResponse create(AuthenticatedUser authenticatedUser, CreateOrderRequest request) {
		OrderJdbcRepository.OrderContext context = resolveContext(authenticatedUser, request);
		UUID orderId = orderJdbcRepository.insertOrder(authenticatedUser.businessId(), context.customerId(),
				context.leadId(), context.conversationId(), authenticatedUser.userId(),
				normalizeStatus(request.status(), "DRAFT"),
				orderJdbcRepository.findBusinessCurrency(authenticatedUser.businessId()),
				normalizeMoney(request.discountAmount()), request.dueDate(), clean(request.notes()));
		orderJdbcRepository.addItems(authenticatedUser.businessId(), orderId, normalizeItems(request.items()));
		orderJdbcRepository.recalculateTotals(authenticatedUser.businessId(), orderId);
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional
	public OrderDetailResponse createFromConversation(AuthenticatedUser authenticatedUser, UUID conversationId,
			CreateOrderRequest request) {
		OrderJdbcRepository.OrderContext context = orderJdbcRepository
				.findConversationContext(authenticatedUser.businessId(), conversationId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro la conversacion solicitada."));
		CreateOrderRequest resolved = new CreateOrderRequest(context.customerId(), context.leadId(), conversationId,
				context.customerName(), context.customerPhone(), context.customerEmail(), request.status(),
				request.discountAmount(), request.dueDate(), request.notes(), request.items());
		return create(authenticatedUser, resolved);
	}

	@Transactional
	public OrderDetailResponse createFromLead(AuthenticatedUser authenticatedUser, UUID leadId,
			CreateOrderRequest request) {
		OrderJdbcRepository.OrderContext context = orderJdbcRepository
				.findLeadContext(authenticatedUser.businessId(), leadId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro el prospecto solicitado."));
		CreateOrderRequest resolved = new CreateOrderRequest(context.customerId(), leadId, context.conversationId(),
				context.customerName(), context.customerPhone(), context.customerEmail(), request.status(),
				request.discountAmount(), request.dueDate(), request.notes(), request.items());
		return create(authenticatedUser, resolved);
	}

	@Transactional
	public OrderDetailResponse update(AuthenticatedUser authenticatedUser, UUID orderId, UpdateOrderRequest request) {
		OrderDetailResponse current = orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
		orderJdbcRepository.updateOrder(authenticatedUser.businessId(), orderId,
				normalizeStatus(request.status(), current.status()), normalizeMoney(request.discountAmount()),
				request.dueDate(), clean(request.notes()));
		if (request.items() != null) {
			orderJdbcRepository.replaceItems(authenticatedUser.businessId(), orderId, normalizeItems(request.items()));
		}
		orderJdbcRepository.recalculateTotals(authenticatedUser.businessId(), orderId);
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional
	public OrderDetailResponse addProducts(AuthenticatedUser authenticatedUser, UUID orderId,
			List<CreateOrderItemRequest> items) {
		orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
		orderJdbcRepository.addItems(authenticatedUser.businessId(), orderId, normalizeItems(items));
		orderJdbcRepository.recalculateTotals(authenticatedUser.businessId(), orderId);
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional
	public OrderDetailResponse updateStatus(AuthenticatedUser authenticatedUser, UUID orderId, String status) {
		orderJdbcRepository.updateOrderStatus(authenticatedUser.businessId(), orderId, normalizeStatus(status, null));
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional
	public OrderDetailResponse registerPayment(AuthenticatedUser authenticatedUser, UUID orderId,
			RegisterPaymentRequest request) {
		orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
		orderJdbcRepository.registerPayment(authenticatedUser.businessId(), orderId, request.amount(),
				clean(request.method()) == null ? "TRANSFER" : clean(request.method()).toUpperCase(Locale.ROOT),
				request.paidAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : request.paidAt(),
				clean(request.reference()), clean(request.notes()));
		orderJdbcRepository.recalculateTotals(authenticatedUser.businessId(), orderId);
		return orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
	}

	@Transactional(readOnly = true)
	public SendOrderSummaryResponse sendSummary(AuthenticatedUser authenticatedUser, UUID orderId) {
		OrderDetailResponse order = orderJdbcRepository.findOrderDetail(authenticatedUser.businessId(), orderId);
		String body = buildWhatsappSummary(order);
		ChannelDispatchResponse dispatchResponse = channelDispatchService.dispatch(new ChannelDispatchRequest(
				authenticatedUser.businessId(), MessageChannelType.WHATSAPP, order.customerPhone(), body));
		return new SendOrderSummaryResponse(dispatchResponse.status(), dispatchResponse.externalMessageId(),
				dispatchResponse.acceptedAt(), body);
	}

	private OrderJdbcRepository.OrderContext resolveContext(AuthenticatedUser authenticatedUser,
			CreateOrderRequest request) {
		if (request.conversationId() != null) {
			return orderJdbcRepository.findConversationContext(authenticatedUser.businessId(), request.conversationId())
					.orElseThrow(() -> new ResourceNotFoundException("No se encontro la conversacion solicitada."));
		}
		if (request.leadId() != null) {
			return orderJdbcRepository.findLeadContext(authenticatedUser.businessId(), request.leadId())
					.orElseThrow(() -> new ResourceNotFoundException("No se encontro el prospecto solicitado."));
		}
		if (request.customerId() != null) {
			OrderJdbcRepository.CustomerContext customer = orderJdbcRepository
					.findCustomer(authenticatedUser.businessId(), request.customerId())
					.orElseThrow(() -> new ResourceNotFoundException("No se encontro el cliente solicitado."));
			return new OrderJdbcRepository.OrderContext(customer.id(), customer.displayName(), customer.phone(),
					customer.email(), null, null);
		}
		if (request.customerName() == null || request.customerName().isBlank() || request.customerPhone() == null
				|| request.customerPhone().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_CUSTOMER_REQUIRED",
					"Debes informar cliente existente o nombre y telefono para crear el pedido.");
		}
		OrderJdbcRepository.CustomerContext customer = orderJdbcRepository.findOrCreateCustomer(
				authenticatedUser.businessId(), request.customerName(), request.customerPhone(),
				request.customerEmail());
		return new OrderJdbcRepository.OrderContext(customer.id(), customer.displayName(), customer.phone(),
				customer.email(), null, null);
	}

	private List<CreateOrderItemRequest> normalizeItems(List<CreateOrderItemRequest> items) {
		if (items == null) {
			return List.of();
		}
		for (CreateOrderItemRequest item : items) {
			if (item.quantity() <= 0) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_ITEM_QUANTITY_INVALID",
						"La cantidad de cada producto debe ser mayor a cero.");
			}
		}
		return items;
	}

	private String normalizeStatus(String status, String defaultStatus) {
		String resolved = status == null || status.isBlank() ? defaultStatus : status.trim().toUpperCase(Locale.ROOT);
		if (resolved == null || !ORDER_STATUSES.contains(resolved)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_STATUS_INVALID",
					"Estado de pedido no valido. Usa DRAFT, CONFIRMED, PREPARING, READY, DELIVERED o CANCELLED.");
		}
		return resolved;
	}

	private String normalizeOptionalStatus(String status) {
		return status == null || status.isBlank() ? null : normalizeStatus(status, null);
	}

	private String normalizeOptionalPaymentStatus(String paymentStatus) {
		if (paymentStatus == null || paymentStatus.isBlank()) {
			return null;
		}
		String resolved = paymentStatus.trim().toUpperCase(Locale.ROOT);
		if (!Set.of("PENDING", "PAID", "PARTIAL").contains(resolved)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PAYMENT_STATUS_INVALID",
					"Estado de pago no valido. Usa PENDING, PAID o PARTIAL.");
		}
		return resolved;
	}

	private BigDecimal normalizeMoney(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String buildWhatsappSummary(OrderDetailResponse order) {
		StringBuilder builder = new StringBuilder();
		builder.append("Hola ").append(order.customerName()).append(", este es el resumen de tu pedido ")
				.append(order.orderNumber()).append(":\n");
		order.items()
				.forEach(item -> builder.append("- ").append(item.productName()).append(" x").append(item.quantity())
						.append(" = ").append(order.currency()).append(" ").append(item.lineTotal()).append("\n"));
		builder.append("Total: ").append(order.currency()).append(" ").append(order.totalAmount()).append("\n");
		builder.append("Pagado: ").append(order.currency()).append(" ").append(order.paidAmount()).append("\n");
		builder.append("Saldo: ").append(order.currency()).append(" ").append(order.balanceDue()).append("\n");
		builder.append("Estado: ").append(order.status()).append(" / Pago: ").append(order.paymentStatus()).append(".");
		return builder.toString();
	}
}
