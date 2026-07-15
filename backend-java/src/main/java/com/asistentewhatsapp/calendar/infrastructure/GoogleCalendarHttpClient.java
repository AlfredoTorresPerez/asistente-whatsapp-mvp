package com.asistentewhatsapp.calendar.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleCalendarHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarHttpClient.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
    private static final String CALENDAR_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/%s/events";
    private static final String CALENDAR_EVENT_URL = "https://www.googleapis.com/calendar/v3/calendars/%s/events/%s";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleCalendarHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    public TokenResponse exchangeAuthorizationCode(String code, String redirectUri, String clientId, String clientSecret) {
        String body = "grant_type=authorization_code"
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret);

        String response = executePostFormUrlEncoded(TOKEN_URL, body);
        return parseResponse(response, TokenResponse.class);
    }

    public RefreshTokenResponse refreshAccessToken(String refreshToken, String clientId, String clientSecret) {
        String body = "grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(refreshToken)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret);

        String response = executePostFormUrlEncoded(TOKEN_URL, body);
        return parseResponse(response, RefreshTokenResponse.class);
    }

    public void revokeToken(String token) {
        String body = "token=" + urlEncode(token);
        try {
            String response = executePostFormUrlEncoded(REVOKE_URL, body);
            LOGGER.debug("GOOGLE_REVOKE_SUCCESS");
        } catch (GoogleCalendarApiException e) {
            if (e.getStatusCode() == 400) {
                // Token already revoked or invalid
                LOGGER.debug("GOOGLE_REVOKE_ALREADY_INVALID statusCode={}", e.getStatusCode());
                return;
            }
            throw e;
        }
    }

    public UserInfoResponse getUserInfo(String accessToken) {
        String response = executeGet(USERINFO_URL, accessToken);
        return parseResponse(response, UserInfoResponse.class);
    }

    public CalendarListResponse listCalendarList(String accessToken) {
        String response = executeGet(CALENDAR_LIST_URL, accessToken);
        return parseResponse(response, CalendarListResponse.class);
    }

    public String createEvent(String calendarId, String body, String accessToken) {
        String url = String.format(CALENDAR_EVENTS_URL, urlEncode(calendarId));
        return executePostJson(url, body, accessToken);
    }

    public String updateEvent(String calendarId, String eventId, String body, String accessToken) {
        String url = String.format(CALENDAR_EVENT_URL, urlEncode(calendarId), urlEncode(eventId));
        return executePutJson(url, body, accessToken);
    }

    public String getEvent(String calendarId, String eventId, String accessToken) {
        String url = String.format(CALENDAR_EVENT_URL, urlEncode(calendarId), urlEncode(eventId));
        return executeGet(url, accessToken);
    }

    public void deleteEvent(String calendarId, String eventId, String accessToken) {
        String url = String.format(CALENDAR_EVENT_URL, urlEncode(calendarId), urlEncode(eventId));
        executeDelete(url, accessToken);
    }

    private String executePostFormUrlEncoded(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return executeRequest(request);
    }

    private String executePostJson(String url, String body, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return executeRequest(request);
    }

    private String executePutJson(String url, String body, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return executeRequest(request);
    }

    private String executeGet(String url, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return executeRequest(request);
    }

    private String executeDelete(String url, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();
        return executeRequest(request);
    }

    private String executeRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            }

            String errorDetail = extractErrorMessage(responseBody);
            LOGGER.warn("GOOGLE_API_ERROR statusCode={} url={} error={}", statusCode, request.uri(), errorDetail);

            throw new GoogleCalendarApiException(statusCode, errorDetail, responseBody);
        } catch (GoogleCalendarApiException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            throw new GoogleCalendarApiException(0, "Request timed out", null);
        } catch (Exception e) {
            throw new GoogleCalendarApiException(0, "Request failed: " + e.getMessage(), null);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "Unknown error";
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode error = node.get("error");
            if (error != null) {
                if (error.isTextual()) return error.asText();
                JsonNode message = error.get("message");
                if (message != null) return message.asText();
            }
            return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
        } catch (Exception e) {
            return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
        }
    }

    private <T> T parseResponse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new GoogleCalendarApiException(0, "Failed to parse response: " + e.getMessage(), json);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record TokenResponse(String accessToken, String refreshToken, Long expiresIn, String scope) {
        public TokenResponse {
            if (accessToken == null) accessToken = "";
            if (scope == null) scope = "";
        }
    }

    public record RefreshTokenResponse(String accessToken, Long expiresIn) {}

    public record UserInfoResponse(String id, String email, Boolean verifiedEmail, String name, String picture) {}

    public record CalendarListResponse(List<CalendarListEntry> items) {
        public CalendarListResponse {
            if (items == null) items = List.of();
        }
    }

    public record CalendarListEntry(String id, String summary, Boolean primary, String accessRole) {
        public CalendarListEntry {
            if (id == null) id = "";
            if (summary == null) summary = "";
            if (primary == null) primary = false;
            if (accessRole == null) accessRole = "reader";
        }

        public boolean isWritable() {
            return "writer".equals(accessRole) || "owner".equals(accessRole);
        }
    }

    public static class GoogleCalendarApiException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public GoogleCalendarApiException(int statusCode, String message, String responseBody) {
            super("Google Calendar API error [" + statusCode + "]: " + message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() { return statusCode; }
        public String getResponseBody() { return responseBody; }

        public boolean isAuthError() {
            return statusCode == 401 || statusCode == 403;
        }

        public boolean isNotFound() {
            return statusCode == 404 || statusCode == 410;
        }

        public boolean isConflict() {
            return statusCode == 409;
        }

        public boolean isRateLimit() {
            return statusCode == 429;
        }

        public boolean isServerError() {
            return statusCode >= 500 || statusCode == 0;
        }

        public boolean isPermanentError() {
            return statusCode == 400 || statusCode == 403;
        }
    }
}
