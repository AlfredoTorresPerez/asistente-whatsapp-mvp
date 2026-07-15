package com.asistentewhatsapp.shared.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlackNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackNotifier.class);

    private final HttpClient httpClient;

    public SlackNotifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void notifyError(String message, String details) {
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.equals("__SLACK_WEBHOOK_URL__")) {
            return;
        }

        String payload = """
                {"text": "[ASISTENTE] *ERROR:* %s\\n```%s```"}
                """.formatted(escapeJson(message), escapeJson(details));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            LOGGER.warn("Error enviando notificacion Slack: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
