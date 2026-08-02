package com.asistentewhatsapp.landing.application;

import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.landing.api.PublicCenterResponse;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.Benefit;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.CompanyInfo;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.LocationItem;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.PageConfig;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.PromotionItem;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.ServiceItem;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.Testimonial;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.WhatsAppInfo;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.domain.BusinessEntity;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicCenterService {

	private static final Logger log = LoggerFactory.getLogger(PublicCenterService.class);

	private final BusinessRepository businessRepository;
	private final MetaOnboardingRepository metaOnboardingRepository;
	private final BusinessLocationJdbcRepository locationRepository;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public PublicCenterService(BusinessRepository businessRepository, MetaOnboardingRepository metaOnboardingRepository,
			BusinessLocationJdbcRepository locationRepository, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.businessRepository = businessRepository;
		this.metaOnboardingRepository = metaOnboardingRepository;
		this.locationRepository = locationRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PublicCenterResponse getCenterBySlug(String slug) {
		BusinessEntity business = businessRepository.findByCode(slug)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND",
						"No se encontro el centro con slug: " + slug));

		UUID businessId = business.getId();

		CompanyInfo company = new CompanyInfo(business.getCompanyName(), business.getBusinessName(), null,
				business.getContactEmail(), business.getSupportPhone(), business.getAddress());

		List<ServiceItem> services = findServices(businessId);
		List<PromotionItem> promotions = findPromotions(businessId);
		List<LocationItem> locations = findLocations(businessId);
		WhatsAppInfo whatsapp = buildWhatsAppInfo(business);
		PageConfig pageConfig = findPageConfig(businessId);

		return new PublicCenterResponse(company, services, promotions, locations, whatsapp, pageConfig);
	}

	@Transactional(readOnly = true)
	public WhatsAppInfo getWhatsAppInfo(String slug) {
		BusinessEntity business = businessRepository.findByCode(slug)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND",
						"No se encontro el centro con slug: " + slug));
		return buildWhatsAppInfo(business);
	}

	public void registerClick(String slug, String sourceIp, String userAgent, String referer) {
		businessRepository.findByCode(slug).ifPresent(business -> {
			try {
				jdbcTemplate.update(
						"insert into whatsapp_click_log (id, business_id, slug, source_ip, user_agent, referer) values (?, ?, ?, ?, ?, ?)",
						UUID.randomUUID(), business.getId(), slug, sourceIp, userAgent, referer);
			} catch (Exception e) {
				log.warn("Failed to register whatsapp click for slug={}: {}", slug, e.getMessage());
			}
		});
	}

	@Transactional
	public String saveContact(String slug, String name, String phone) {
		BusinessEntity business = businessRepository.findByCode(slug)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CENTER_NOT_FOUND",
						"No se encontro el centro con slug: " + slug));

		UUID businessId = business.getId();
		String cleanPhone = phone.replaceAll("\\D", "");
		String firstName = name.trim();
		String lastName = "";

		NameParts parts = splitName(firstName);
		firstName = parts.first();
		lastName = parts.last();

		UUID customerId = findOrCreateCustomer(businessId, firstName, lastName, cleanPhone);
		UUID leadId = UUID.randomUUID();

		jdbcTemplate.update(
				"""
						insert into lead (id, business_id, customer_id, source_type, first_name, last_name, phone, normalized_phone, stage, active)
						values (?, ?, ?, 'LANDING_PAGE', ?, ?, ?, ?, 'NEW', true)
						""",
				leadId, businessId, customerId, firstName, lastName, cleanPhone, cleanPhone);

		WhatsAppInfo whatsapp = buildWhatsAppInfo(business);
		if (whatsapp == null) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_WHATSAPP",
					"El centro no tiene WhatsApp configurado");
		}
		return whatsapp.waUrl();
	}

	private UUID findOrCreateCustomer(UUID businessId, String firstName, String lastName, String phone) {
		List<UUID> existing = jdbcTemplate.query(
				"select id from customer where business_id = ? and normalized_phone = ? and active = true",
				(rs, rn) -> rs.getObject("id", UUID.class), businessId, phone);
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}
		UUID customerId = UUID.randomUUID();
		String displayName = (firstName + " " + lastName).trim();
		jdbcTemplate.update(
				"""
						insert into customer (id, business_id, first_name, last_name, display_name, phone, normalized_phone, active)
						values (?, ?, ?, ?, ?, ?, ?, true)
						""",
				customerId, businessId, firstName, lastName, displayName, phone, phone);
		return customerId;
	}

	private record NameParts(String first, String last) {
	}

	private NameParts splitName(String fullName) {
		int idx = fullName.indexOf(' ');
		if (idx == -1) {
			return new NameParts(fullName, "");
		}
		return new NameParts(fullName.substring(0, idx), fullName.substring(idx + 1).trim());
	}

	private List<ServiceItem> findServices(UUID businessId) {
		return jdbcTemplate.query("""
				select s.id, s.name, s.description, c.code as category_code, c.name as category_name,
				       s.duration_minutes, s.price_base
				from aesthetic_service s
				left join aesthetic_service_category c on c.id = s.category_id
				where s.business_id = ? and s.active = true
				order by s.name asc
				""",
				(rs, rn) -> new ServiceItem(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getString("description"), rs.getString("category_code"), rs.getString("category_name"),
						rs.getInt("duration_minutes"), rs.getBigDecimal("price_base")),
				businessId);
	}

	private List<PromotionItem> findPromotions(UUID businessId) {
		LocalDate today = LocalDate.now();
		return jdbcTemplate.query("""
				select id, name, description, discount_type, discount_value, starts_on, ends_on
				from aesthetic_promotion
				where business_id = ? and active = true
				  and (starts_on is null or starts_on <= ?)
				  and (ends_on is null or ends_on >= ?)
				order by ends_on asc nulls last, name asc
				""", (rs, rn) -> {
			java.sql.Date startsOn = rs.getDate("starts_on");
			java.sql.Date endsOn = rs.getDate("ends_on");
			return new PromotionItem(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"),
					rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
					startsOn != null ? startsOn.toLocalDate() : null, endsOn != null ? endsOn.toLocalDate() : null);
		}, businessId, today, today);
	}

	private List<LocationItem> findLocations(UUID businessId) {
		return locationRepository.findActive(businessId).stream().map(
				l -> new LocationItem(l.id(), l.name(), l.address(), l.city(), l.commune(), l.phone(), l.timezone()))
				.toList();
	}

	/**
	 * Resuelve el numero de WhatsApp del centro con la siguiente prioridad: 1.
	 * Numero del canal META_CLOUD_API configurado (phone/normalized). 2. Numero de
	 * la empresa (business.support_phone). 3. Numero visible del canal
	 * (display_phone_number). 4. WhatsApp de la sede por defecto
	 * (business_location.whatsapp_number).
	 */
	private WhatsAppInfo buildWhatsAppInfo(BusinessEntity business) {
		ChannelAccountRecord channel = metaOnboardingRepository.findCloudApiChannel(business.getId()).orElse(null);
		String channelPhone = channel != null
				? firstNonBlank(channel.phoneNumber(), channel.normalizedPhoneNumber())
				: null;
		String channelDisplay = channel != null ? channel.displayPhoneNumber() : null;
		BusinessLocationRecord location = locationRepository.findDefaultActive(business.getId()).orElse(null);
		String locationWhatsapp = location != null ? location.whatsappNumber() : null;

		String phone = resolveWhatsAppPhone(channelPhone, business.getSupportPhone(), channelDisplay, locationWhatsapp);
		if (phone == null || phone.isBlank()) {
			return null;
		}
		String cleanPhone = phone.replaceAll("\\D", "");
		String message = "Hola, quiero más información";
		String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
		String waUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;

		return new WhatsAppInfo(waUrl, cleanPhone, channelDisplay != null ? channelDisplay : phone, message);
	}

	static String resolveWhatsAppPhone(String channelPhone, String businessSupportPhone, String channelDisplayPhone,
			String locationWhatsapp) {
		return firstNonBlank(channelPhone, businessSupportPhone, channelDisplayPhone, locationWhatsapp);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private PageConfig findPageConfig(UUID businessId) {
		try {
			return jdbcTemplate.queryForObject("""
					select primary_color, secondary_color,
					       welcome_title, welcome_subtitle,
					       about_title, about_text,
					       benefits, testimonials,
					       header_logo_url, hero_image_url,
					       show_services, show_promotions, show_testimonials
					from business_public_config
					where business_id = ? and active = true
					""", (rs, rn) -> {
				List<Benefit> benefits = parseJsonList(rs.getString("benefits"), Benefit.class);
				List<Testimonial> testimonials = parseJsonList(rs.getString("testimonials"), Testimonial.class);
				return new PageConfig(rs.getString("primary_color"), rs.getString("secondary_color"),
						rs.getString("welcome_title"), rs.getString("welcome_subtitle"), rs.getString("about_title"),
						rs.getString("about_text"), benefits, testimonials, rs.getString("header_logo_url"),
						rs.getString("hero_image_url"), rs.getBoolean("show_services"),
						rs.getBoolean("show_promotions"), rs.getBoolean("show_testimonials"));
			}, businessId);
		} catch (Exception e) {
			log.debug("No public config found for business {}: {}", businessId, e.getMessage());
			return defaultPageConfig();
		}
	}

	private PageConfig defaultPageConfig() {
		return new PageConfig("#EC4899", "#8B5CF6", null, null, null, null, Collections.emptyList(),
				Collections.emptyList(), null, null, true, true, true);
	}

	private <T> List<T> parseJsonList(String json, Class<T> elementType) {
		if (json == null || json.isBlank())
			return Collections.emptyList();
		try {
			return objectMapper.readValue(json,
					objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
		} catch (Exception e) {
			log.warn("Failed to parse JSON list for type {}: {}", elementType.getSimpleName(), e.getMessage());
			return Collections.emptyList();
		}
	}
}
