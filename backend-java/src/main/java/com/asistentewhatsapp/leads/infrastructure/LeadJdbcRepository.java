package com.asistentewhatsapp.leads.infrastructure;

import com.asistentewhatsapp.leads.api.LeadDetailResponse;
import com.asistentewhatsapp.leads.api.LeadNoteResponse;
import com.asistentewhatsapp.leads.api.LeadSummaryResponse;
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
public class LeadJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public LeadJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public PagedResponse<LeadSummaryResponse> findLeads(UUID businessId, int page, int size, String search,
			String stage, String origin, UUID responsibleUserId) {
		QueryParts queryParts = buildLeadListQuery(businessId, search, stage, origin, responsibleUserId);
		Long totalItems = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotalItems == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);

		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<LeadSummaryResponse> items = jdbcTemplate.query("""
				select
				    l.id,
				    l.customer_id,
				    l.conversation_id,
				    l.first_name,
				    l.last_name,
				    concat(l.first_name, ' ', l.last_name) as display_name,
				    l.phone,
				    l.email,
				    l.stage,
				    l.source_type,
				    l.assigned_user_id,
				    case
				        when ua.id is null then null
				        else concat(ua.first_name, ' ', ua.last_name)
				    end as assigned_user_name,
				    l.created_at,
				    l.updated_at
				""" + queryParts.fromAndWhere() + """
				order by l.updated_at desc, l.created_at desc
				limit :limit
				offset :offset
				""", parameters, leadSummaryRowMapper());

		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
	}

	public LeadDetailResponse findLeadDetail(UUID businessId, UUID leadId) {
		List<LeadDetailRow> rows = jdbcTemplate.query("""
				select
				    l.id,
				    l.customer_id,
				    l.conversation_id,
				    l.first_name,
				    l.last_name,
				    concat(l.first_name, ' ', l.last_name) as display_name,
				    l.phone,
				    l.email,
				    l.stage,
				    l.source_type,
				    l.notes,
				    l.assigned_user_id,
				    case
				        when ua.id is null then null
				        else concat(ua.first_name, ' ', ua.last_name)
				    end as assigned_user_name,
				    l.active,
				    l.created_at,
				    l.updated_at
				from lead l
				left join user_account ua on ua.id = l.assigned_user_id
				where l.business_id = :businessId
				  and l.id = :leadId
				""", leadParameters(businessId, leadId), leadDetailRowMapper());

		if (rows.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el prospecto solicitado.");
		}

		LeadDetailRow row = rows.getFirst();
		return new LeadDetailResponse(row.id(), row.customerId(), row.conversationId(), row.firstName(), row.lastName(),
				row.displayName(), row.phone(), row.email(), row.stage(), row.sourceType(), row.notes(),
				row.assignedUserId(), row.assignedUserName(), row.active(), row.createdAt(), row.updatedAt(),
				findLeadNotes(businessId, leadId));
	}

	public LeadContextRecord findLeadContext(UUID businessId, UUID leadId) {
		List<LeadContextRecord> items = jdbcTemplate.query("""
				select
				    id,
				    customer_id,
				    conversation_id,
				    source_type
				from lead
				where business_id = :businessId
				  and id = :leadId
				""", leadParameters(businessId, leadId),
				(resultSet, rowNum) -> new LeadContextRecord(resultSet.getObject("id", UUID.class),
						resultSet.getObject("customer_id", UUID.class),
						resultSet.getObject("conversation_id", UUID.class), resultSet.getString("source_type")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el prospecto solicitado.");
		}
		return items.getFirst();
	}

	public ConversationLeadContextRecord findConversationLeadContext(UUID businessId, UUID conversationId) {
		List<ConversationLeadContextRecord> items = jdbcTemplate.query("""
				select
				    c.id,
				    c.customer_id,
				    c.assigned_user_id,
				    c.customer_name,
				    c.customer_phone,
				    cu.first_name as customer_first_name,
				    cu.last_name as customer_last_name,
				    cu.email as customer_email
				from conversation c
				join customer cu on cu.id = c.customer_id
				where c.business_id = :businessId
				  and c.id = :conversationId
				""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
						conversationId),
				(resultSet, rowNum) -> new ConversationLeadContextRecord(resultSet.getObject("id", UUID.class),
						resultSet.getObject("customer_id", UUID.class),
						resultSet.getObject("assigned_user_id", UUID.class), resultSet.getString("customer_name"),
						resultSet.getString("customer_phone"), resultSet.getString("customer_first_name"),
						resultSet.getString("customer_last_name"), resultSet.getString("customer_email")));
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
		}
		return items.getFirst();
	}

	public boolean existsLeadForConversation(UUID businessId, UUID conversationId) {
		Long total = jdbcTemplate.queryForObject("""
				select count(*)
				from lead
				where business_id = :businessId
				  and conversation_id = :conversationId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
				conversationId), Long.class);
		return total != null && total > 0;
	}

	public Optional<UUID> findLeadIdByConversation(UUID businessId, UUID conversationId) {
		List<UUID> items = jdbcTemplate.query("""
				select id
				from lead
				where business_id = :businessId
				  and conversation_id = :conversationId
				order by active desc, updated_at desc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
				conversationId), (resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
		return items.stream().findFirst();
	}

	public Optional<UUID> findActiveLeadIdByNormalizedPhone(UUID businessId, String normalizedPhone) {
		List<UUID> items = jdbcTemplate.query("""
				select id
				from lead
				where business_id = :businessId
				  and normalized_phone = :normalizedPhone
				  and active = true
				order by updated_at desc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("normalizedPhone",
				normalizedPhone), (resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
		return items.stream().findFirst();
	}

	public void linkLeadToConversationIfUnlinked(UUID businessId, UUID leadId, UUID conversationId) {
		jdbcTemplate.update("""
				update lead
				set conversation_id = :conversationId,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :leadId
				  and conversation_id is null
				""", leadParameters(businessId, leadId).addValue("conversationId", conversationId));
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

	public UUID insertLead(UUID businessId, UUID customerId, UUID conversationId, String sourceType, String firstName,
			String lastName, String phone, String email, String stage, String notes, UUID assignedUserId) {
		UUID leadId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into lead (
				    id,
				    business_id,
				    customer_id,
				    conversation_id,
				    source_type,
				    first_name,
				    last_name,
				    phone,
				    normalized_phone,
				    email,
				    stage,
				    notes,
				    assigned_user_id,
				    active
				) values (
				    :id,
				    :businessId,
				    :customerId,
				    :conversationId,
				    :sourceType,
				    :firstName,
				    :lastName,
				    :phone,
				    :normalizedPhone,
				    :email,
				    :stage,
				    :notes,
				    :assignedUserId,
				    true
				)
				""", new MapSqlParameterSource().addValue("id", leadId).addValue("businessId", businessId)
				.addValue("customerId", customerId).addValue("conversationId", conversationId)
				.addValue("sourceType", sourceType).addValue("firstName", firstName).addValue("lastName", lastName)
				.addValue("phone", phone).addValue("normalizedPhone", phone).addValue("email", email)
				.addValue("stage", stage).addValue("notes", notes).addValue("assignedUserId", assignedUserId));
		return leadId;
	}

	public void updateLead(UUID businessId, UUID leadId, UUID customerId, String firstName, String lastName,
			String phone, String email, String stage, String notes, UUID assignedUserId) {
		int updated = jdbcTemplate.update("""
				update lead
				set customer_id = :customerId,
				    first_name = :firstName,
				    last_name = :lastName,
				    phone = :phone,
				    normalized_phone = :normalizedPhone,
				    email = :email,
				    stage = :stage,
				    notes = :notes,
				    assigned_user_id = :assignedUserId,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :leadId
				""",
				leadParameters(businessId, leadId).addValue("customerId", customerId).addValue("firstName", firstName)
						.addValue("lastName", lastName).addValue("phone", phone).addValue("normalizedPhone", phone)
						.addValue("email", email).addValue("stage", stage).addValue("notes", notes)
						.addValue("assignedUserId", assignedUserId));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el prospecto solicitado.");
		}
	}

	public void updateLeadStage(UUID businessId, UUID leadId, String stage) {
		int updated = jdbcTemplate.update("""
				update lead
				set stage = :stage,
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :leadId
				""", leadParameters(businessId, leadId).addValue("stage", stage));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el prospecto solicitado.");
		}
	}

	public UUID insertLeadNote(UUID businessId, UUID leadId, UUID authorUserId, String noteText) {
		UUID noteId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into lead_note (
				    id,
				    business_id,
				    lead_id,
				    author_user_id,
				    note_text
				) values (
				    :id,
				    :businessId,
				    :leadId,
				    :authorUserId,
				    :noteText
				)
				""", leadParameters(businessId, leadId).addValue("id", noteId).addValue("authorUserId", authorUserId)
				.addValue("noteText", noteText));
		return noteId;
	}

	public LeadNoteResponse findLeadNoteById(UUID businessId, UUID leadId, UUID noteId) {
		List<LeadNoteResponse> items = jdbcTemplate.query("""
				select
				    ln.id,
				    ln.author_user_id,
				    concat(ua.first_name, ' ', ua.last_name) as author_user_name,
				    ln.note_text,
				    ln.created_at,
				    ln.updated_at
				from lead_note ln
				join user_account ua on ua.id = ln.author_user_id
				where ln.business_id = :businessId
				  and ln.lead_id = :leadId
				  and ln.id = :noteId
				""", leadParameters(businessId, leadId).addValue("noteId", noteId), leadNoteRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la nota solicitada.");
		}
		return items.getFirst();
	}

	public List<LeadNoteResponse> findLeadNotes(UUID businessId, UUID leadId) {
		return jdbcTemplate.query("""
				select
				    ln.id,
				    ln.author_user_id,
				    concat(ua.first_name, ' ', ua.last_name) as author_user_name,
				    ln.note_text,
				    ln.created_at,
				    ln.updated_at
				from lead_note ln
				join user_account ua on ua.id = ln.author_user_id
				where ln.business_id = :businessId
				  and ln.lead_id = :leadId
				order by ln.created_at desc
				""", leadParameters(businessId, leadId), leadNoteRowMapper());
	}

	private QueryParts buildLeadListQuery(UUID businessId, String search, String stage, String origin,
			UUID responsibleUserId) {
		StringBuilder sql = new StringBuilder("""
				from lead l
				left join user_account ua on ua.id = l.assigned_user_id
				where l.business_id = :businessId
				""");
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);

		if (search != null) {
			sql.append("""
					 and (
					    l.first_name ilike :search
					    or l.last_name ilike :search
					    or l.phone ilike :search
					    or coalesce(l.email, '') ilike :search
					    or coalesce(l.notes, '') ilike :search
					 )
					""");
			parameters.addValue("search", "%" + search + "%");
		}

		if (stage != null) {
			sql.append(" and l.stage = :stage ");
			parameters.addValue("stage", stage);
		}

		if (origin != null) {
			sql.append(" and l.source_type = :origin ");
			parameters.addValue("origin", origin);
		}

		if (responsibleUserId != null) {
			sql.append(" and l.assigned_user_id = :responsibleUserId ");
			parameters.addValue("responsibleUserId", responsibleUserId);
		}

		return new QueryParts(sql.toString(), parameters);
	}

	private MapSqlParameterSource leadParameters(UUID businessId, UUID leadId) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("leadId", leadId);
	}

	private RowMapper<LeadSummaryResponse> leadSummaryRowMapper() {
		return (resultSet, rowNum) -> new LeadSummaryResponse(resultSet.getObject("id", UUID.class),
				resultSet.getObject("customer_id", UUID.class), resultSet.getObject("conversation_id", UUID.class),
				resultSet.getString("first_name"), resultSet.getString("last_name"),
				resultSet.getString("display_name"), resultSet.getString("phone"), resultSet.getString("email"),
				resultSet.getString("stage"), resultSet.getString("source_type"),
				resultSet.getObject("assigned_user_id", UUID.class), resultSet.getString("assigned_user_name"),
				resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<LeadDetailRow> leadDetailRowMapper() {
		return (resultSet, rowNum) -> new LeadDetailRow(resultSet.getObject("id", UUID.class),
				resultSet.getObject("customer_id", UUID.class), resultSet.getObject("conversation_id", UUID.class),
				resultSet.getString("first_name"), resultSet.getString("last_name"),
				resultSet.getString("display_name"), resultSet.getString("phone"), resultSet.getString("email"),
				resultSet.getString("stage"), resultSet.getString("source_type"), resultSet.getString("notes"),
				resultSet.getObject("assigned_user_id", UUID.class), resultSet.getString("assigned_user_name"),
				resultSet.getBoolean("active"), resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<LeadNoteResponse> leadNoteRowMapper() {
		return (resultSet, rowNum) -> new LeadNoteResponse(resultSet.getObject("id", UUID.class),
				resultSet.getObject("author_user_id", UUID.class), resultSet.getString("author_user_name"),
				resultSet.getString("note_text"), resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<CustomerRecord> customerRowMapper() {
		return (resultSet, rowNum) -> new CustomerRecord(resultSet.getObject("id", UUID.class),
				resultSet.getString("first_name"), resultSet.getString("last_name"),
				resultSet.getString("display_name"), resultSet.getString("phone"), resultSet.getString("email"));
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}

	private record LeadDetailRow(UUID id, UUID customerId, UUID conversationId, String firstName, String lastName,
			String displayName, String phone, String email, String stage, String sourceType, String notes,
			UUID assignedUserId, String assignedUserName, boolean active, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	public record CustomerRecord(UUID id, String firstName, String lastName, String displayName, String phone,
			String email) {
	}

	public record LeadContextRecord(UUID id, UUID customerId, UUID conversationId, String sourceType) {
	}

	public record ConversationLeadContextRecord(UUID id, UUID customerId, UUID assignedUserId,
			String customerDisplayName, String customerPhone, String customerFirstName, String customerLastName,
			String customerEmail) {
	}
}
