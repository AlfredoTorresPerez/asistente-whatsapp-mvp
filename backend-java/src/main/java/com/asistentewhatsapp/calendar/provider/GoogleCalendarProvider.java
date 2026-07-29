package com.asistentewhatsapp.calendar.provider;

import com.asistentewhatsapp.calendar.CalendarEventData;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.GoogleCalendarApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCalendarProvider implements CalendarProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarProvider.class);
	private static final String PROVIDER_NAME = "GOOGLE";
	private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events"
			+ " https://www.googleapis.com/auth/calendar.readonly" + " https://www.googleapis.com/auth/userinfo.email";
	private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	private final GoogleCalendarHttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;
	private final boolean enabled;

	public GoogleCalendarProvider(GoogleCalendarHttpClient httpClient,
			@Value("${app.calendar.google.client-id:}") String clientId,
			@Value("${app.calendar.google.client-secret:}") String clientSecret,
			@Value("${app.calendar.google.redirect-uri:}") String redirectUri,
			@Value("${app.calendar.google.enabled:false}") boolean enabled) {
		this.httpClient = httpClient;
		this.objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
		this.enabled = enabled;
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getAuthUrl(String state, String redirectUri) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		return AUTH_URL + "?client_id=" + urlEncode(clientId) + "&redirect_uri="
				+ urlEncode(redirectUri != null ? redirectUri : this.redirectUri) + "&response_type=code" + "&scope="
				+ urlEncode(SCOPE) + "&access_type=offline" + "&prompt=consent" + "&state=" + urlEncode(state);
	}

	@Override
	public TokenExchangeResult exchangeCode(String code, String effectiveRedirectUri) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		String usedRedirectUri = effectiveRedirectUri != null ? effectiveRedirectUri : this.redirectUri;
		GoogleCalendarHttpClient.TokenResponse tokenResponse = httpClient.exchangeAuthorizationCode(code,
				usedRedirectUri, clientId, clientSecret);
		return new TokenExchangeResult(tokenResponse.accessToken(), tokenResponse.refreshToken(),
				tokenResponse.expiresIn(), null, null);
	}

	@Override
	public RefreshResult refreshAccessToken(String refreshToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		GoogleCalendarHttpClient.RefreshTokenResponse response = httpClient.refreshAccessToken(refreshToken, clientId,
				clientSecret);
		return new RefreshResult(response.accessToken(), response.expiresIn());
	}

	@Override
	public RevokeResult revokeToken(String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		try {
			httpClient.revokeToken(accessToken);
			return new RevokeResult(true, null);
		} catch (GoogleCalendarApiException e) {
			LOGGER.warn("GOOGLE_REVOKE_FAILED statusCode={}", e.getStatusCode());
			return new RevokeResult(false, e.getMessage());
		}
	}

	@Override
	public UserInfoResult getUserInfo(String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		GoogleCalendarHttpClient.UserInfoResponse userInfo = httpClient.getUserInfo(accessToken);
		return new UserInfoResult(userInfo.id(), userInfo.email(),
				userInfo.verifiedEmail() != null && userInfo.verifiedEmail(), userInfo.name());
	}

	@Override
	public List<CalendarListEntry> listCalendars(String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		GoogleCalendarHttpClient.CalendarListResponse response = httpClient.listCalendarList(accessToken);
		List<CalendarListEntry> result = new ArrayList<>();
		for (GoogleCalendarHttpClient.CalendarListEntry entry : response.items()) {
			if (entry.isWritable()) {
				result.add(new CalendarListEntry(entry.id(), entry.summary(), entry.primary(), entry.accessRole()));
			}
		}
		return result;
	}

	@Override
	public String createEvent(String calendarId, CalendarEventData eventData, String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		String idempotentEventId = computeIdempotentId(eventData.businessId(), eventData.bookingId());
		String body = buildEventJson(eventData, idempotentEventId);
		String response = httpClient.createEvent(calendarId, body, accessToken);
		try {
			JsonNode root = objectMapper.readTree(response);
			String eventId = root.has("id") ? root.get("id").asText() : null;
			if (eventId == null) {
				throw new GoogleCalendarApiException(0, "No id in createEvent response", response);
			}
			return eventId;
		} catch (GoogleCalendarApiException e) {
			throw e;
		} catch (Exception e) {
			throw new GoogleCalendarApiException(0, "Failed to parse createEvent response: " + e.getMessage(),
					response);
		}
	}

	@Override
	public void updateEvent(String calendarId, String externalEventId, CalendarEventData eventData,
			String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		String body = buildEventJson(eventData, null);
		httpClient.updateEvent(calendarId, externalEventId, body, accessToken);
	}

	@Override
	public void deleteEvent(String calendarId, String externalEventId, String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		httpClient.deleteEvent(calendarId, externalEventId, accessToken);
	}

	@Override
	public CalendarEventData getEvent(String calendarId, String externalEventId, String accessToken) {
		if (!enabled) {
			throw new IllegalStateException("Google Calendar integration is not enabled");
		}
		String response = httpClient.getEvent(calendarId, externalEventId, accessToken);
		try {
			JsonNode root = objectMapper.readTree(response);
			String summary = root.has("summary") ? root.get("summary").asText() : null;
			String description = root.has("description") ? root.get("description").asText() : null;
			String location = root.has("location") ? root.get("location").asText() : null;
			OffsetDateTime startAt = parseDateTime(root, "start");
			OffsetDateTime endAt = parseDateTime(root, "end");
			String timezone = root.has("start") && root.get("start").has("timeZone")
					? root.get("start").get("timeZone").asText()
					: null;
			String attendeeEmail = null;
			String attendeeName = null;
			if (root.has("attendees") && root.get("attendees").isArray() && root.get("attendees").size() > 0) {
				JsonNode attendee = root.get("attendees").get(0);
				attendeeEmail = attendee.has("email") ? attendee.get("email").asText() : null;
				attendeeName = attendee.has("displayName") ? attendee.get("displayName").asText() : null;
			}
			UUID businessId = null;
			UUID bookingId = null;
			if (root.has("extendedProperties") && root.get("extendedProperties").has("private")) {
				JsonNode priv = root.get("extendedProperties").get("private");
				if (priv.has("businessId")) {
					try {
						businessId = UUID.fromString(priv.get("businessId").asText());
					} catch (Exception ignored) {
					}
				}
				if (priv.has("bookingId")) {
					try {
						bookingId = UUID.fromString(priv.get("bookingId").asText());
					} catch (Exception ignored) {
					}
				}
			}
			return new CalendarEventData(summary, description, location, startAt, endAt, timezone, attendeeEmail,
					attendeeName, businessId, bookingId, externalEventId);
		} catch (GoogleCalendarApiException e) {
			throw e;
		} catch (Exception e) {
			throw new GoogleCalendarApiException(0, "Failed to parse getEvent response: " + e.getMessage(), response);
		}
	}

	private String buildEventJson(CalendarEventData eventData, String idempotentEventId) {
		ObjectNode root = objectMapper.createObjectNode();

		if (idempotentEventId != null) {
			root.put("id", idempotentEventId);
		}

		if (eventData.summary() != null) {
			root.put("summary", "Reserva \u2014 " + eventData.summary());
		}

		if (eventData.description() != null) {
			root.put("description", eventData.description());
		}

		if (eventData.location() != null) {
			root.put("location", eventData.location());
		}

		ObjectNode startNode = objectMapper.createObjectNode();
		if (eventData.startAt() != null) {
			startNode.put("dateTime", eventData.startAt().format(RFC3339_FORMATTER));
		}
		if (eventData.timezone() != null) {
			startNode.put("timeZone", eventData.timezone());
		}
		root.set("start", startNode);

		ObjectNode endNode = objectMapper.createObjectNode();
		if (eventData.endAt() != null) {
			endNode.put("dateTime", eventData.endAt().format(RFC3339_FORMATTER));
		}
		if (eventData.timezone() != null) {
			endNode.put("timeZone", eventData.timezone());
		}
		root.set("end", endNode);

		if (eventData.businessId() != null || eventData.bookingId() != null) {
			ObjectNode privateProps = objectMapper.createObjectNode();
			if (eventData.businessId() != null) {
				privateProps.put("businessId", eventData.businessId().toString());
			}
			if (eventData.bookingId() != null) {
				privateProps.put("bookingId", eventData.bookingId().toString());
			}
			ObjectNode extendedProperties = objectMapper.createObjectNode();
			extendedProperties.set("private", privateProps);
			root.set("extendedProperties", extendedProperties);
		}

		if (eventData.attendeeEmail() != null && !eventData.attendeeEmail().isBlank()) {
			ArrayNode attendees = objectMapper.createArrayNode();
			ObjectNode attendee = objectMapper.createObjectNode();
			attendee.put("email", eventData.attendeeEmail());
			if (eventData.attendeeName() != null) {
				attendee.put("displayName", eventData.attendeeName());
			}
			attendees.add(attendee);
			root.set("attendees", attendees);
		}

		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			throw new RuntimeException("Failed to build event JSON", e);
		}
	}

	private OffsetDateTime parseDateTime(JsonNode parent, String field) {
		if (!parent.has(field))
			return null;
		JsonNode node = parent.get(field);
		if (!node.has("dateTime"))
			return null;
		try {
			return OffsetDateTime.parse(node.get("dateTime").asText());
		} catch (Exception e) {
			return null;
		}
	}

	private String computeIdempotentId(UUID businessId, UUID bookingId) {
		if (businessId == null || bookingId == null)
			return null;
		String input = businessId.toString() + "|" + bookingId.toString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			String hex = HexFormat.of().formatHex(hash);
			return hex.substring(0, 40);
		} catch (Exception e) {
			throw new RuntimeException("Failed to compute idempotent event ID", e);
		}
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
