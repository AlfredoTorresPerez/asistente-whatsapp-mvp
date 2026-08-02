package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.api.BookingEmailLogResponse;
import com.asistentewhatsapp.bookings.api.BookingPaymentResponse;
import com.asistentewhatsapp.bookings.api.BookingPublicLinkSummaryResponse;
import com.asistentewhatsapp.bookings.api.BookingReminderResponse;
import com.asistentewhatsapp.bookings.api.BookingSummaryResponse;
import com.asistentewhatsapp.bookings.api.BookingStatusHistoryResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingJdbcRepository {

	private static final List<String> ACTIVE_BOOKING_STATUSES = List.of("REQUESTED", "TEMPORARY",
			"PENDIENTE_CONFIRMACION", "CONFIRMED", "RESCHEDULED", "REPROGRAMADA", "SOLICITADA", "PENDIENTE_PAGO",
			"CONFIRMADA", "REPROGRAMACION_PENDIENTE");

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public BookingJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public PagedResponse<BookingSummaryResponse> findBookings(UUID businessId, int page, int size, OffsetDateTime from,
			OffsetDateTime to, String search, String status, UUID responsibleUserId) {
		QueryParts queryParts = buildListQuery(businessId, from, to, search, status, responsibleUserId);
		Long totalItems = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotalItems == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);

		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<BookingSummaryResponse> items = jdbcTemplate.query("""
				select
				    b.id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    b.duration_minutes,
				    b.location_id,
				    b.location,
				    bl.name as location_name,
				    c.id as customer_id,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    b.lead_id,
				    b.conversation_id,
				    b.assigned_user_id,
				    b.requires_deposit,
				    coalesce(b.deposit_amount, 0) as deposit_amount,
				    b.payment_status,
				    case
				        when ua.id is null then null
				        else concat(ua.first_name, ' ', ua.last_name)
				    end as assigned_user_name,
				    coalesce(bcs.sync_status, 'NONE') as calendar_sync_status
				""" + queryParts.fromAndWhere() + """
				order by b.starts_at asc, b.created_at desc
				limit :limit
				offset :offset
				""", parameters, bookingSummaryRowMapper());

		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
	}

	public BookingDetailResponse findBookingDetail(UUID businessId, UUID bookingId) {
		List<BookingDetailResponse> items = jdbcTemplate.query("""
				select
				    b.id,
				    b.subject,
				    b.status,
				    b.starts_at,
				    b.duration_minutes,
				    b.location_id,
				    b.location,
				    bl.name as location_name,
				    b.service_id,
				    b.professional_id,
				    b.room_id,
				    b.notes,
				    b.completed_at,
				    b.created_at,
				    b.updated_at,
				    c.id as customer_id,
				    c.display_name as customer_name,
				    c.phone as customer_phone,
				    c.email as customer_email,
				    b.lead_id,
				    b.conversation_id,
				    b.assigned_user_id,
				    b.requires_deposit,
				    coalesce(b.deposit_amount, 0) as deposit_amount,
				    b.payment_status,
				    case
				        when ua.id is null then null
				        else concat(ua.first_name, ' ', ua.last_name)
				    end as assigned_user_name
				from booking b
				join customer c
				  on c.id = b.customer_id
				 and c.business_id = b.business_id
				left join user_account ua
				  on ua.id = b.assigned_user_id
				left join business_location bl
				  on bl.id = b.location_id
				 and bl.business_id = b.business_id
				where b.business_id = :businessId
				  and b.id = :bookingId
				""", bookingParameters(businessId, bookingId), bookingDetailRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la cita solicitada.");
		}
		BookingDetailResponse base = items.getFirst();
		return new BookingDetailResponse(base.id(), base.subject(), normalizeStatusForApi(base.status()),
				base.startsAt(), base.durationMinutes(), base.locationId(), base.location(), base.locationName(),
				base.serviceId(), base.professionalId(), base.roomId(), base.notes(), base.completedAt(),
				base.createdAt(), base.updatedAt(), base.customerId(), base.customerName(), base.customerPhone(),
				base.customerEmail(), base.leadId(), base.conversationId(), base.assignedUserId(),
				base.assignedUserName(), base.requiresDeposit(), base.depositAmount(), base.paymentStatus(),
				findStatusHistory(businessId, bookingId), findPublicLinks(businessId, bookingId),
				findReminders(businessId, bookingId), findEmailLogs(businessId, bookingId),
				findPayments(businessId, bookingId));
	}

	public List<BookingStatusHistoryResponse> findStatusHistory(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query("""
				select id, previous_status, new_status, reason, actor_user_id, source, created_at
				from booking_status_history
				where business_id = :businessId
				  and booking_id = :bookingId
				order by created_at desc
				""", bookingParameters(businessId, bookingId),
				(resultSet, rowNum) -> new BookingStatusHistoryResponse(resultSet.getObject("id", UUID.class),
						normalizeStatusForApi(resultSet.getString("previous_status")),
						normalizeStatusForApi(resultSet.getString("new_status")), resultSet.getString("reason"),
						resultSet.getObject("actor_user_id", UUID.class), resultSet.getString("source"),
						resultSet.getObject("created_at", OffsetDateTime.class)));
	}

	public List<BookingPublicLinkSummaryResponse> findPublicLinks(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query(
				"""
						select id, type, status, url, expires_at, used_at, created_at
						from (
						    select id, 'CONFIRMATION' as type, status, confirmation_url as url, expires_at, confirmed_at as used_at, created_at
						    from booking_confirmation_link
						    where business_id = :businessId and booking_id = :bookingId
						    union all
						    select id, 'RESCHEDULE' as type, status, reschedule_url as url, expires_at, used_at, created_at
						    from booking_reschedule_link
						    where business_id = :businessId and booking_id = :bookingId
						    union all
						    select id, 'CANCELLATION' as type, status, cancellation_url as url, expires_at, used_at, created_at
						    from booking_cancellation_link
						    where business_id = :businessId and booking_id = :bookingId
						) links
						order by created_at desc
						""",
				bookingParameters(businessId, bookingId),
				(resultSet, rowNum) -> new BookingPublicLinkSummaryResponse(resultSet.getObject("id", UUID.class),
						resultSet.getString("type"), resultSet.getString("status"), resultSet.getString("url"),
						resultSet.getObject("expires_at", OffsetDateTime.class),
						resultSet.getObject("used_at", OffsetDateTime.class),
						resultSet.getObject("created_at", OffsetDateTime.class)));
	}

	public List<BookingReminderResponse> findReminders(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query("""
				select id, reminder_type, channel_type, scheduled_at, sent_at, status,
				       coalesce(failure_reason, error_message) as failure_reason, template_key
				from booking_reminder
				where business_id = :businessId
				  and booking_id = :bookingId
				order by scheduled_at asc, channel_type asc
				""", bookingParameters(businessId, bookingId),
				(resultSet, rowNum) -> new BookingReminderResponse(resultSet.getObject("id", UUID.class),
						resultSet.getString("reminder_type"), resultSet.getString("channel_type"),
						resultSet.getObject("scheduled_at", OffsetDateTime.class),
						resultSet.getObject("sent_at", OffsetDateTime.class),
						normalizeReminderStatusForApi(resultSet.getString("status")),
						resultSet.getString("failure_reason"), resultSet.getString("template_key")));
	}

	public List<BookingEmailLogResponse> findEmailLogs(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query(
				"""
						select id, recipient_email, subject, template_key, status, simulation, failure_reason, sent_at, created_at
						from booking_email_log
						where business_id = :businessId
						  and booking_id = :bookingId
						order by created_at desc
						""",
				bookingParameters(businessId, bookingId),
				(resultSet, rowNum) -> new BookingEmailLogResponse(resultSet.getObject("id", UUID.class),
						resultSet.getString("recipient_email"), resultSet.getString("subject"),
						resultSet.getString("template_key"), resultSet.getString("status"),
						resultSet.getBoolean("simulation"), resultSet.getString("failure_reason"),
						resultSet.getObject("sent_at", OffsetDateTime.class),
						resultSet.getObject("created_at", OffsetDateTime.class)));
	}

	public List<BookingPaymentResponse> findPayments(UUID businessId, UUID bookingId) {
		return jdbcTemplate.query("""
				select id, booking_id, provider, provider_payment_id, idempotency_key,
				       amount, currency, status, checkout_url, checkout_expires_at, manual,
				       approved_at, rejected_at, expired_at, refunded_at, created_at
				from booking_payment
				where business_id = :businessId
				  and booking_id = :bookingId
				order by created_at desc
				""", bookingParameters(businessId, bookingId),
				(resultSet, rowNum) -> new BookingPaymentResponse(resultSet.getObject("id", UUID.class),
						resultSet.getObject("booking_id", UUID.class), resultSet.getString("provider"),
						resultSet.getString("provider_payment_id"), resultSet.getString("idempotency_key"),
						resultSet.getBigDecimal("amount"), resultSet.getString("currency"),
						resultSet.getString("status"), resultSet.getString("checkout_url"),
						resultSet.getObject("checkout_expires_at", OffsetDateTime.class),
						resultSet.getBoolean("manual"), resultSet.getObject("approved_at", OffsetDateTime.class),
						resultSet.getObject("rejected_at", OffsetDateTime.class),
						resultSet.getObject("expired_at", OffsetDateTime.class),
						resultSet.getObject("refunded_at", OffsetDateTime.class),
						resultSet.getObject("created_at", OffsetDateTime.class)));
	}

	public Optional<UUID> findUserId(UUID businessId, UUID userId) {
		List<UUID> items = jdbcTemplate.query("""
				select id
				from user_account
				where business_id = :businessId
				  and id = :userId
				  and status = 'ACTIVE'
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("userId", userId),
				(resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
		return items.stream().findFirst();
	}

	public Optional<CustomerRecord> findCustomerByNormalizedPhone(UUID businessId, String normalizedPhone) {
		List<CustomerRecord> items = jdbcTemplate.query("""
				select
				    id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    email
				from customer
				where business_id = :businessId
				  and normalized_phone = :normalizedPhone
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("normalizedPhone",
				normalizedPhone), customerRowMapper());
		return items.stream().findFirst();
	}

	public CustomerRecord findCustomerById(UUID businessId, UUID customerId) {
		List<CustomerRecord> items = jdbcTemplate.query("""
				select
				    id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    email
				from customer
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId),
				customerRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el cliente asociado.");
		}
		return items.getFirst();
	}

	public UUID insertCustomer(UUID businessId, String firstName, String lastName, String displayName, String phone,
			String email) {
		UUID customerId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into customer (
				    id,
				    business_id,
				    first_name,
				    last_name,
				    display_name,
				    phone,
				    normalized_phone,
				    email,
				    active
				) values (
				    :id,
				    :businessId,
				    :firstName,
				    :lastName,
				    :displayName,
				    :phone,
				    :normalizedPhone,
				    :email,
				    true
				)
				""", new MapSqlParameterSource().addValue("id", customerId).addValue("businessId", businessId)
				.addValue("firstName", firstName).addValue("lastName", lastName).addValue("displayName", displayName)
				.addValue("phone", phone).addValue("normalizedPhone", phone).addValue("email", email));
		return customerId;
	}

	public void updateCustomer(UUID businessId, UUID customerId, String firstName, String lastName, String displayName,
			String phone, String email) {
		int updated = jdbcTemplate.update("""
				update customer
				set first_name = :firstName,
				    last_name = :lastName,
				    display_name = :displayName,
				    phone = :phone,
				    normalized_phone = :normalizedPhone,
				    email = :email,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId)
				.addValue("firstName", firstName).addValue("lastName", lastName).addValue("displayName", displayName)
				.addValue("phone", phone).addValue("normalizedPhone", phone).addValue("email", email));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el cliente asociado.");
		}
	}

	public ConversationContextRecord findConversationContext(UUID businessId, UUID conversationId) {
		List<ConversationContextRecord> items = jdbcTemplate.query("""
				select
				    c.id,
				    c.customer_id,
				    c.assigned_user_id,
				    c.location_id,
				    bl.name as location_name,
				    c.customer_name,
				    c.customer_phone,
				    cu.first_name as customer_first_name,
				    cu.last_name as customer_last_name,
				    cu.email as customer_email
				from conversation c
				join customer cu
				  on cu.id = c.customer_id
				 and cu.business_id = c.business_id
				left join business_location bl
				  on bl.id = c.location_id
				 and bl.business_id = c.business_id
				where c.business_id = :businessId
				  and c.id = :conversationId
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
						conversationId),
				(resultSet, rowNum) -> new ConversationContextRecord(resultSet.getObject("id", UUID.class),
						resultSet.getObject("customer_id", UUID.class),
						resultSet.getObject("assigned_user_id", UUID.class),
						resultSet.getObject("location_id", UUID.class), resultSet.getString("location_name"),
						resultSet.getString("customer_name"), resultSet.getString("customer_phone"),
						resultSet.getString("customer_first_name"), resultSet.getString("customer_last_name"),
						resultSet.getString("customer_email")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
		}
		return items.getFirst();
	}

	public LeadContextRecord findLeadContext(UUID businessId, UUID leadId) {
		List<LeadContextRecord> items = jdbcTemplate.query("""
				select
				    l.id,
				    l.customer_id,
				    l.conversation_id,
				    l.assigned_user_id,
				    l.first_name,
				    l.last_name,
				    l.phone,
				    l.email
				from lead l
				where l.business_id = :businessId
				  and l.id = :leadId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("leadId", leadId),
				(resultSet, rowNum) -> new LeadContextRecord(resultSet.getObject("id", UUID.class),
						resultSet.getObject("customer_id", UUID.class),
						resultSet.getObject("conversation_id", UUID.class),
						resultSet.getObject("assigned_user_id", UUID.class), resultSet.getString("first_name"),
						resultSet.getString("last_name"), resultSet.getString("phone"), resultSet.getString("email")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el prospecto solicitado.");
		}
		return items.getFirst();
	}

	public Optional<UUID> findLeadIdByConversation(UUID businessId, UUID conversationId) {
		List<UUID> items = jdbcTemplate.query("""
				select id
				from lead
				where business_id = :businessId
				  and conversation_id = :conversationId
				order by created_at desc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
				conversationId), (resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
		return items.stream().findFirst();
	}

	public UUID insertBooking(UUID businessId, UUID customerId, UUID leadId, UUID conversationId, UUID assignedUserId,
			String subject, String status, OffsetDateTime startsAt, int durationMinutes, UUID locationId,
			String location, String notes, OffsetDateTime completedAt) {
		UUID bookingId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into booking (
				    id,
				    business_id,
				    customer_id,
				    lead_id,
				    conversation_id,
				    assigned_user_id,
				    subject,
				    status,
				    starts_at,
				    ends_at,
				    duration_minutes,
				    location_id,
				    location,
				    notes,
				    completed_at
				) values (
				    :id,
				    :businessId,
				    :customerId,
				    :leadId,
				    :conversationId,
				    :assignedUserId,
				    :subject,
				    :status,
				    :startsAt,
				    :endsAt,
				    :durationMinutes,
				    :locationId,
				    :location,
				    :notes,
				    :completedAt
				)
				""",
				new MapSqlParameterSource().addValue("id", bookingId).addValue("businessId", businessId)
						.addValue("customerId", customerId).addValue("leadId", leadId)
						.addValue("conversationId", conversationId).addValue("assignedUserId", assignedUserId)
						.addValue("subject", subject).addValue("status", status).addValue("startsAt", startsAt)
						.addValue("endsAt", startsAt.plusMinutes(durationMinutes))
						.addValue("durationMinutes", durationMinutes).addValue("locationId", locationId)
						.addValue("location", location).addValue("notes", notes).addValue("completedAt", completedAt));
		return bookingId;
	}

	public void updateBooking(UUID businessId, UUID bookingId, UUID assignedUserId, String subject, String status,
			OffsetDateTime startsAt, int durationMinutes, UUID locationId, String location, String notes,
			OffsetDateTime completedAt) {
		int updated = jdbcTemplate.update("""
				update booking
				set assigned_user_id = :assignedUserId,
				    subject = :subject,
				    status = :status,
				    starts_at = :startsAt,
				    ends_at = :endsAt,
				    duration_minutes = :durationMinutes,
				    location_id = :locationId,
				    location = :location,
				    notes = :notes,
				    completed_at = :completedAt,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :bookingId
				  and status in (:activeStatuses)
				""", bookingParameters(businessId, bookingId).addValue("assignedUserId", assignedUserId)
				.addValue("subject", subject).addValue("status", status).addValue("startsAt", startsAt)
				.addValue("endsAt", startsAt.plusMinutes(durationMinutes)).addValue("durationMinutes", durationMinutes)
				.addValue("locationId", locationId).addValue("location", location).addValue("notes", notes)
				.addValue("completedAt", completedAt).addValue("activeStatuses", ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la cita solicitada.");
		}
	}

	public void rescheduleBooking(UUID businessId, UUID bookingId, OffsetDateTime startsAt, int durationMinutes,
			UUID locationId, String location, String notes) {
		int updated = jdbcTemplate.update("""
				update booking
				set status = 'REPROGRAMADA',
				    starts_at = :startsAt,
				    ends_at = :endsAt,
				    duration_minutes = :durationMinutes,
				    location_id = :locationId,
				    location = :location,
				    notes = :notes,
				    completed_at = null,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :bookingId
				  and status in (:activeStatuses)
				""", bookingParameters(businessId, bookingId).addValue("startsAt", startsAt)
				.addValue("endsAt", startsAt.plusMinutes(durationMinutes)).addValue("durationMinutes", durationMinutes)
				.addValue("locationId", locationId).addValue("location", location).addValue("notes", notes)
				.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro una cita activa para reprogramar.");
		}
	}

	public void cancelBooking(UUID businessId, UUID bookingId, String notes) {
		int updated = jdbcTemplate.update("""
				update booking
				set status = 'CANCELADA',
				    cancelled_at = coalesce(cancelled_at, current_timestamp),
				    notes = :notes,
				    completed_at = null,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :bookingId
				  and status in (:activeStatuses)
				""", bookingParameters(businessId, bookingId).addValue("notes", notes).addValue("activeStatuses",
				ACTIVE_BOOKING_STATUSES));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro una cita activa para cancelar.");
		}
	}

	private QueryParts buildListQuery(UUID businessId, OffsetDateTime from, OffsetDateTime to, String search,
			String status, UUID responsibleUserId) {
		StringBuilder sql = new StringBuilder("""
				from booking b
				join customer c
				  on c.id = b.customer_id
				 and c.business_id = b.business_id
				left join user_account ua
				  on ua.id = b.assigned_user_id
				left join business_location bl
				  on bl.id = b.location_id
				 and bl.business_id = b.business_id
				left join booking_calendar_sync bcs
				  on bcs.booking_id = b.id
				 and bcs.business_id = b.business_id
				where b.business_id = :businessId
				  and b.starts_at < :to
				  and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :from
				""");
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("from", from).addValue("to", to);

		if (search != null) {
			sql.append("""
					 and (
					    b.subject ilike :search
					    or c.display_name ilike :search
					    or c.phone ilike :search
					    or coalesce(b.location, '') ilike :search
					    or coalesce(bl.name, '') ilike :search
					    or coalesce(b.notes, '') ilike :search
					 )
					""");
			parameters.addValue("search", "%" + search + "%");
		}

		if (status != null) {
			sql.append(" and b.status = :status ");
			parameters.addValue("status", status);
		} else {
			sql.append(" and b.status in (:activeStatuses) ");
			parameters.addValue("activeStatuses", ACTIVE_BOOKING_STATUSES);
		}

		if (responsibleUserId != null) {
			sql.append(" and b.assigned_user_id = :responsibleUserId ");
			parameters.addValue("responsibleUserId", responsibleUserId);
		}

		return new QueryParts(sql.toString(), parameters);
	}

	private MapSqlParameterSource bookingParameters(UUID businessId, UUID bookingId) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId);
	}

	private RowMapper<BookingSummaryResponse> bookingSummaryRowMapper() {
		return (resultSet, rowNum) -> new BookingSummaryResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("subject"), resultSet.getString("status"),
				resultSet.getObject("starts_at", OffsetDateTime.class), resultSet.getInt("duration_minutes"),
				resultSet.getObject("location_id", UUID.class), resultSet.getString("location"),
				resultSet.getString("location_name"), resultSet.getObject("customer_id", UUID.class),
				resultSet.getString("customer_name"), resultSet.getString("customer_phone"),
				resultSet.getObject("lead_id", UUID.class), resultSet.getObject("conversation_id", UUID.class),
				resultSet.getObject("assigned_user_id", UUID.class), resultSet.getString("assigned_user_name"),
				resultSet.getBoolean("requires_deposit"), resultSet.getBigDecimal("deposit_amount"),
				resultSet.getString("payment_status"), resultSet.getString("calendar_sync_status"));
	}

	private RowMapper<BookingDetailResponse> bookingDetailRowMapper() {
		return (resultSet, rowNum) -> new BookingDetailResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("subject"), resultSet.getString("status"),
				resultSet.getObject("starts_at", OffsetDateTime.class), resultSet.getInt("duration_minutes"),
				resultSet.getObject("location_id", UUID.class), resultSet.getString("location"),
				resultSet.getString("location_name"), resultSet.getObject("service_id", UUID.class),
				resultSet.getObject("professional_id", UUID.class), resultSet.getObject("room_id", UUID.class),
				resultSet.getString("notes"), resultSet.getObject("completed_at", OffsetDateTime.class),
				resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class), resultSet.getObject("customer_id", UUID.class),
				resultSet.getString("customer_name"), resultSet.getString("customer_phone"),
				resultSet.getString("customer_email"), resultSet.getObject("lead_id", UUID.class),
				resultSet.getObject("conversation_id", UUID.class), resultSet.getObject("assigned_user_id", UUID.class),
				resultSet.getString("assigned_user_name"), resultSet.getBoolean("requires_deposit"),
				resultSet.getBigDecimal("deposit_amount"), resultSet.getString("payment_status"), List.of(), List.of(),
				List.of(), List.of(), List.of());
	}

	private RowMapper<CustomerRecord> customerRowMapper() {
		return (resultSet, rowNum) -> new CustomerRecord(resultSet.getObject("id", UUID.class),
				resultSet.getString("first_name"), resultSet.getString("last_name"),
				resultSet.getString("display_name"), resultSet.getString("phone"), resultSet.getString("email"));
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}

	public record CustomerRecord(UUID id, String firstName, String lastName, String displayName, String phone,
			String email) {
	}

	public record ConversationContextRecord(UUID id, UUID customerId, UUID assignedUserId, UUID locationId,
			String locationName, String customerDisplayName, String customerPhone, String customerFirstName,
			String customerLastName, String customerEmail) {
	}

	public record LeadContextRecord(UUID id, UUID customerId, UUID conversationId, UUID assignedUserId,
			String firstName, String lastName, String phone, String email) {
	}

	private String normalizeStatusForApi(String status) {
		if (status == null) {
			return null;
		}
		return switch (status) {
			case "RESCHEDULED" -> "REPROGRAMADA";
			case "CANCELLED" -> "CANCELADA";
			case "CONFIRMED" -> "CONFIRMADA";
			case "REQUESTED" -> "SOLICITADA";
			case "COMPLETED", "ATTENDED" -> "ATENDIDA";
			case "NO_SHOW" -> "NO_ASISTE";
			case "EXPIRED", "RELEASED" -> "EXPIRADA";
			default -> status;
		};
	}

	private String normalizeReminderStatusForApi(String status) {
		if ("SCHEDULED".equals(status)) {
			return "PENDING";
		}
		return status;
	}
}
