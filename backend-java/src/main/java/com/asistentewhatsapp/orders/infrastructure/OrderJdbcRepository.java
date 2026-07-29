package com.asistentewhatsapp.orders.infrastructure;

import com.asistentewhatsapp.orders.api.CreateOrderItemRequest;
import com.asistentewhatsapp.orders.api.OrderDetailResponse;
import com.asistentewhatsapp.orders.api.OrderItemResponse;
import com.asistentewhatsapp.orders.api.OrderPaymentResponse;
import com.asistentewhatsapp.orders.api.OrderSummaryResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public OrderJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public PagedResponse<OrderSummaryResponse> findOrders(UUID businessId, int page, int size, String search,
			String status, String paymentStatus) {
		QueryParts queryParts = orderQuery(businessId, search, status, paymentStatus);
		Long total = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		long totalItems = total == null ? 0 : total;
		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);
		List<OrderSummaryResponse> items = jdbcTemplate.query(summarySelect() + queryParts.fromAndWhere() + """
				order by o.created_at desc
				limit :limit
				offset :offset
				""", parameters, summaryRowMapper());
		return new PagedResponse<>(items, page, size, totalItems, totalPages(totalItems, size));
	}

	public OrderDetailResponse findOrderDetail(UUID businessId, UUID orderId) {
		List<OrderSummaryRecord> summaries = jdbcTemplate.query(recordSelect() + """
				from order_request o
				join customer c on c.id = o.customer_id and c.business_id = o.business_id
				where o.business_id = :businessId
				  and o.id = :orderId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId),
				orderRecordRowMapper());
		if (summaries.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el pedido solicitado.");
		}
		OrderSummaryRecord order = summaries.getFirst();
		List<OrderItemResponse> items = findItems(businessId, orderId);
		List<OrderPaymentResponse> payments = findPayments(businessId, orderId);
		return new OrderDetailResponse(order.id(), order.orderNumber(), order.customerId(), order.customerName(),
				order.customerPhone(), order.leadId(), order.conversationId(), order.status(), order.paymentStatus(),
				order.subtotalAmount(), order.discountAmount(), order.totalAmount(), order.paidAmount(),
				order.balanceDue(), order.currency(), order.dueDate(), order.notes(), order.createdAt(),
				order.updatedAt(), items, payments, buildReceiptPreview(order, items, payments));
	}

	public UUID insertOrder(UUID businessId, UUID customerId, UUID leadId, UUID conversationId, UUID createdByUserId,
			String status, String currency, BigDecimal discountAmount, LocalDate dueDate, String notes) {
		UUID orderId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into order_request (
				    id, business_id, customer_id, lead_id, conversation_id, created_by_user_id,
				    status, payment_status, subtotal_amount, discount_amount, total_amount,
				    paid_amount, balance_due, currency, due_date, notes
				)
				values (
				    :orderId, :businessId, :customerId, :leadId, :conversationId, :createdByUserId,
				    :status, 'PENDING', 0, :discountAmount, 0, 0, 0, :currency, :dueDate, :notes
				)
				""", new MapSqlParameterSource().addValue("orderId", orderId).addValue("businessId", businessId)
				.addValue("customerId", customerId).addValue("leadId", leadId)
				.addValue("conversationId", conversationId).addValue("createdByUserId", createdByUserId)
				.addValue("status", status).addValue("currency", currency).addValue("discountAmount", discountAmount)
				.addValue("dueDate", dueDate).addValue("notes", notes));
		return orderId;
	}

	public void updateOrder(UUID businessId, UUID orderId, String status, BigDecimal discountAmount, LocalDate dueDate,
			String notes) {
		int updated = jdbcTemplate.update("""
				update order_request
				set status = :status,
				    discount_amount = :discountAmount,
				    due_date = :dueDate,
				    notes = :notes,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :orderId
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId)
						.addValue("status", status).addValue("discountAmount", discountAmount)
						.addValue("dueDate", dueDate).addValue("notes", notes));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el pedido solicitado.");
		}
	}

	public void updateOrderStatus(UUID businessId, UUID orderId, String status) {
		int updated = jdbcTemplate.update("""
				update order_request
				set status = :status,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :orderId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId)
				.addValue("status", status));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el pedido solicitado.");
		}
	}

	public void replaceItems(UUID businessId, UUID orderId, List<CreateOrderItemRequest> items) {
		jdbcTemplate.update("""
				update product_service ps
				set stock_quantity = ps.stock_quantity + oi.quantity,
				    updated_at = current_timestamp
				from order_item oi
				where oi.product_service_id = ps.id
				  and oi.business_id = :businessId
				  and oi.order_request_id = :orderId
				  and ps.type = 'PRODUCT'
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId));
		jdbcTemplate.update("""
				delete from order_item
				where business_id = :businessId
				  and order_request_id = :orderId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId));
		addItems(businessId, orderId, items);
	}

	public void addItems(UUID businessId, UUID orderId, List<CreateOrderItemRequest> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		for (CreateOrderItemRequest item : items) {
			ProductRecord product = findProductForSale(businessId, item.productId());
			if (product.stock() < item.quantity()) {
				throw new ResourceNotFoundException("Stock insuficiente para el producto " + product.name() + ".");
			}
			UUID itemId = UUID.randomUUID();
			BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
			jdbcTemplate.update("""
					insert into order_item (
					    id, business_id, order_request_id, product_service_id, product_name_snapshot,
					    sku_snapshot, quantity, unit_price, line_total
					)
					values (
					    :itemId, :businessId, :orderId, :productId, :productName,
					    :sku, :quantity, :unitPrice, :lineTotal
					)
					""",
					new MapSqlParameterSource().addValue("itemId", itemId).addValue("businessId", businessId)
							.addValue("orderId", orderId).addValue("productId", item.productId())
							.addValue("productName", product.name()).addValue("sku", product.sku())
							.addValue("quantity", item.quantity()).addValue("unitPrice", product.price())
							.addValue("lineTotal", lineTotal));
			jdbcTemplate.update("""
					update product_service
					set stock_quantity = stock_quantity - :quantity,
					    updated_at = current_timestamp
					where business_id = :businessId
					  and id = :productId
					  and type = 'PRODUCT'
					""", new MapSqlParameterSource().addValue("businessId", businessId)
					.addValue("productId", item.productId()).addValue("quantity", item.quantity()));
		}
	}

	public void registerPayment(UUID businessId, UUID orderId, BigDecimal amount, String method, OffsetDateTime paidAt,
			String reference, String notes) {
		jdbcTemplate.update("""
				insert into payment (id, business_id, order_request_id, amount, method, paid_at, reference, notes)
				values (:paymentId, :businessId, :orderId, :amount, :method, :paidAt, :reference, :notes)
				""",
				new MapSqlParameterSource().addValue("paymentId", UUID.randomUUID()).addValue("businessId", businessId)
						.addValue("orderId", orderId).addValue("amount", amount).addValue("method", method)
						.addValue("paidAt", paidAt).addValue("reference", reference).addValue("notes", notes));
	}

	public void recalculateTotals(UUID businessId, UUID orderId) {
		jdbcTemplate.update("""
				update order_request o
				set subtotal_amount = totals.subtotal,
				    total_amount = greatest(totals.subtotal - o.discount_amount, 0),
				    paid_amount = payments.paid,
				    balance_due = greatest(greatest(totals.subtotal - o.discount_amount, 0) - payments.paid, 0),
				    payment_status = case
				        when payments.paid <= 0 then 'PENDING'
				        when payments.paid >= greatest(totals.subtotal - o.discount_amount, 0) then 'PAID'
				        else 'PARTIAL'
				    end,
				    updated_at = current_timestamp
				from (
				    select coalesce(sum(line_total), 0) as subtotal
				    from order_item
				    where business_id = :businessId
				      and order_request_id = :orderId
				) totals,
				(
				    select coalesce(sum(amount), 0) as paid
				    from payment
				    where business_id = :businessId
				      and order_request_id = :orderId
				) payments
				where o.business_id = :businessId
				  and o.id = :orderId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId));
	}

	public String findBusinessCurrency(UUID businessId) {
		String currency = jdbcTemplate.queryForObject("""
				select currency from business where id = :businessId
				""", new MapSqlParameterSource().addValue("businessId", businessId), String.class);
		return currency == null ? "CLP" : currency;
	}

	public CustomerContext findOrCreateCustomer(UUID businessId, String customerName, String customerPhone,
			String customerEmail) {
		String normalizedPhone = normalizePhone(customerPhone);
		List<CustomerContext> existing = jdbcTemplate.query("""
				select id, display_name, phone, email
				from customer
				where business_id = :businessId
				  and normalized_phone = :normalizedPhone
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("normalizedPhone",
				normalizedPhone), customerRowMapper());
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}
		UUID customerId = UUID.randomUUID();
		NameParts nameParts = splitName(customerName);
		jdbcTemplate.update(
				"""
						insert into customer (
						    id, business_id, first_name, last_name, display_name, phone, normalized_phone, email, active
						)
						values (
						    :customerId, :businessId, :firstName, :lastName, :displayName, :phone, :normalizedPhone, :email, true
						)
						""",
				new MapSqlParameterSource().addValue("customerId", customerId).addValue("businessId", businessId)
						.addValue("firstName", nameParts.firstName()).addValue("lastName", nameParts.lastName())
						.addValue("displayName", customerName.trim()).addValue("phone", customerPhone.trim())
						.addValue("normalizedPhone", normalizedPhone).addValue("email", clean(customerEmail)));
		return new CustomerContext(customerId, customerName.trim(), customerPhone.trim(), clean(customerEmail));
	}

	public Optional<OrderContext> findConversationContext(UUID businessId, UUID conversationId) {
		List<OrderContext> items = jdbcTemplate.query(
				"""
						select c.id as customer_id, c.display_name, c.phone, c.email, l.id as lead_id, conv.id as conversation_id
						from conversation conv
						join customer c on c.id = conv.customer_id and c.business_id = conv.business_id
						left join lead l on l.conversation_id = conv.id and l.business_id = conv.business_id
						where conv.business_id = :businessId
						  and conv.id = :conversationId
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
						conversationId),
				contextRowMapper());
		return items.stream().findFirst();
	}

	public Optional<OrderContext> findLeadContext(UUID businessId, UUID leadId) {
		List<OrderContext> items = jdbcTemplate.query("""
				select c.id as customer_id, c.display_name, c.phone, c.email, l.id as lead_id, l.conversation_id
				from lead l
				join customer c on c.id = l.customer_id and c.business_id = l.business_id
				where l.business_id = :businessId
				  and l.id = :leadId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("leadId", leadId),
				contextRowMapper());
		return items.stream().findFirst();
	}

	public Optional<CustomerContext> findCustomer(UUID businessId, UUID customerId) {
		List<CustomerContext> items = jdbcTemplate.query("""
				select id, display_name, phone, email
				from customer
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId),
				customerRowMapper());
		return items.stream().findFirst();
	}

	private ProductRecord findProductForSale(UUID businessId, UUID productId) {
		List<ProductRecord> items = jdbcTemplate.query("""
				select id, name, sku, price, stock_quantity
				from product_service
				where business_id = :businessId
				  and id = :productId
				  and type = 'PRODUCT'
				  and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("productId", productId),
				(rs, rowNum) -> new ProductRecord(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getString("sku"), rs.getBigDecimal("price"), rs.getInt("stock_quantity")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el producto activo solicitado.");
		}
		return items.getFirst();
	}

	private List<OrderItemResponse> findItems(UUID businessId, UUID orderId) {
		return jdbcTemplate.query("""
				select id, product_service_id, product_name_snapshot, sku_snapshot, quantity, unit_price, line_total
				from order_item
				where business_id = :businessId
				  and order_request_id = :orderId
				order by created_at asc
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId),
				itemRowMapper());
	}

	private List<OrderPaymentResponse> findPayments(UUID businessId, UUID orderId) {
		return jdbcTemplate.query("""
				select id, amount, method, paid_at, reference, notes
				from payment
				where business_id = :businessId
				  and order_request_id = :orderId
				order by paid_at desc
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("orderId", orderId),
				paymentRowMapper());
	}

	private QueryParts orderQuery(UUID businessId, String search, String status, String paymentStatus) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);
		StringBuilder where = new StringBuilder("""
				from order_request o
				join customer c on c.id = o.customer_id and c.business_id = o.business_id
				where o.business_id = :businessId
				""");
		if (search != null && !search.isBlank()) {
			where.append("""
					and (
					    lower(c.display_name) like :search
					    or lower(c.phone) like :search
					    or lower(o.status) like :search
					)
					""");
			parameters.addValue("search", "%" + search.toLowerCase(Locale.ROOT).trim() + "%");
		}
		if (status != null && !status.isBlank()) {
			where.append(" and o.status = :status\n");
			parameters.addValue("status", status.trim().toUpperCase(Locale.ROOT));
		}
		if (paymentStatus != null && !paymentStatus.isBlank()) {
			where.append(" and o.payment_status = :paymentStatus\n");
			parameters.addValue("paymentStatus", paymentStatus.trim().toUpperCase(Locale.ROOT));
		}
		return new QueryParts(where.toString(), parameters);
	}

	private String summarySelect() {
		return """
				select
				    o.id,
				    concat('#PED-', upper(substr(replace(o.id::text, '-', ''), 1, 8))) as order_number,
				    c.id as customer_id,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    o.lead_id,
				    o.conversation_id,
				    o.status,
				    o.payment_status,
				    o.subtotal_amount,
				    o.discount_amount,
				    o.total_amount,
				    o.paid_amount,
				    o.balance_due,
				    o.currency,
				    o.due_date,
				    o.created_at,
				    o.updated_at
				""";
	}

	private String recordSelect() {
		return """
				select
				    o.id,
				    concat('#PED-', upper(substr(replace(o.id::text, '-', ''), 1, 8))) as order_number,
				    c.id as customer_id,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    o.lead_id,
				    o.conversation_id,
				    o.status,
				    o.payment_status,
				    o.subtotal_amount,
				    o.discount_amount,
				    o.total_amount,
				    o.paid_amount,
				    o.balance_due,
				    o.currency,
				    o.due_date,
				    o.notes,
				    o.created_at,
				    o.updated_at
				""";
	}

	private RowMapper<OrderSummaryResponse> summaryRowMapper() {
		return (rs, rowNum) -> new OrderSummaryResponse(rs.getObject("id", UUID.class), rs.getString("order_number"),
				rs.getObject("customer_id", UUID.class), rs.getString("customer_name"), rs.getString("customer_phone"),
				rs.getObject("lead_id", UUID.class), rs.getObject("conversation_id", UUID.class),
				rs.getString("status"), rs.getString("payment_status"), rs.getBigDecimal("subtotal_amount"),
				rs.getBigDecimal("discount_amount"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("paid_amount"),
				rs.getBigDecimal("balance_due"), rs.getString("currency"), getLocalDate(rs, "due_date"),
				rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<OrderSummaryRecord> orderRecordRowMapper() {
		return (rs, rowNum) -> new OrderSummaryRecord(rs.getObject("id", UUID.class), rs.getString("order_number"),
				rs.getObject("customer_id", UUID.class), rs.getString("customer_name"), rs.getString("customer_phone"),
				rs.getObject("lead_id", UUID.class), rs.getObject("conversation_id", UUID.class),
				rs.getString("status"), rs.getString("payment_status"), rs.getBigDecimal("subtotal_amount"),
				rs.getBigDecimal("discount_amount"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("paid_amount"),
				rs.getBigDecimal("balance_due"), rs.getString("currency"), getLocalDate(rs, "due_date"),
				rs.getString("notes"), rs.getObject("created_at", OffsetDateTime.class),
				rs.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<OrderItemResponse> itemRowMapper() {
		return (rs, rowNum) -> new OrderItemResponse(rs.getObject("id", UUID.class),
				rs.getObject("product_service_id", UUID.class), rs.getString("product_name_snapshot"),
				rs.getString("sku_snapshot"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
				rs.getBigDecimal("line_total"));
	}

	private RowMapper<OrderPaymentResponse> paymentRowMapper() {
		return (rs, rowNum) -> new OrderPaymentResponse(rs.getObject("id", UUID.class), rs.getBigDecimal("amount"),
				rs.getString("method"), rs.getObject("paid_at", OffsetDateTime.class), rs.getString("reference"),
				rs.getString("notes"));
	}

	private RowMapper<CustomerContext> customerRowMapper() {
		return (rs, rowNum) -> new CustomerContext(rs.getObject("id", UUID.class), rs.getString("display_name"),
				rs.getString("phone"), rs.getString("email"));
	}

	private RowMapper<OrderContext> contextRowMapper() {
		return (rs, rowNum) -> new OrderContext(rs.getObject("customer_id", UUID.class), rs.getString("display_name"),
				rs.getString("phone"), rs.getString("email"), rs.getObject("lead_id", UUID.class),
				rs.getObject("conversation_id", UUID.class));
	}

	private String buildReceiptPreview(OrderSummaryRecord order, List<OrderItemResponse> items,
			List<OrderPaymentResponse> payments) {
		List<String> lines = new ArrayList<>();
		lines.add("Comprobante simple " + order.orderNumber());
		lines.add("Cliente: " + order.customerName());
		lines.add("Estado pedido: " + order.status());
		lines.add("Estado pago: " + order.paymentStatus());
		lines.add("Items:");
		for (OrderItemResponse item : items) {
			lines.add("- " + item.productName() + " x" + item.quantity() + " = " + order.currency() + " "
					+ item.lineTotal());
		}
		lines.add("Subtotal: " + order.currency() + " " + order.subtotalAmount());
		lines.add("Descuento: " + order.currency() + " " + order.discountAmount());
		lines.add("Total: " + order.currency() + " " + order.totalAmount());
		lines.add("Pagado: " + order.currency() + " " + order.paidAmount());
		lines.add("Saldo: " + order.currency() + " " + order.balanceDue());
		if (!payments.isEmpty()) {
			lines.add("Ultimo pago: " + payments.getFirst().method() + " " + payments.getFirst().amount());
		}
		return String.join("\n", lines);
	}

	private LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
		java.sql.Date date = rs.getDate(column);
		return date == null ? null : date.toLocalDate();
	}

	private String normalizePhone(String value) {
		return value == null ? "" : value.replaceAll("[^0-9+]", "").trim();
	}

	private NameParts splitName(String displayName) {
		String safe = displayName == null || displayName.isBlank() ? "Cliente" : displayName.trim();
		String[] parts = safe.split("\\s+", 2);
		return new NameParts(parts[0], parts.length > 1 ? parts[1] : "Sin apellido");
	}

	private String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private int totalPages(long totalItems, int size) {
		return totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
	}

	public record CustomerContext(UUID id, String displayName, String phone, String email) {
	}

	public record OrderContext(UUID customerId, String customerName, String customerPhone, String customerEmail,
			UUID leadId, UUID conversationId) {
	}

	private record ProductRecord(UUID id, String name, String sku, BigDecimal price, int stock) {
	}

	private record NameParts(String firstName, String lastName) {
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}

	private record OrderSummaryRecord(UUID id, String orderNumber, UUID customerId, String customerName,
			String customerPhone, UUID leadId, UUID conversationId, String status, String paymentStatus,
			BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal totalAmount, BigDecimal paidAmount,
			BigDecimal balanceDue, String currency, LocalDate dueDate, String notes, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}
}
