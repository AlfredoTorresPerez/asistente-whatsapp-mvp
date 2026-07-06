package com.asistentewhatsapp.shared.email;

import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateRenderer.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final Pattern CLOSING_TAG_PATTERN = Pattern.compile("\\{\\{\\s*/\\w+\\s*}}");
    private static final Pattern OPTIONAL_BLOCK_PATTERN = Pattern.compile(
            "(?s)\\{\\{\\s*(\\w+)\\s*}}(.*?)\\{\\{\\s*/\\1\\s*}}");

    private final JavaMailSender mailSender;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public EmailTemplateRenderer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String render(String templateName, AppointmentConfirmationEmailDTO data) {
        String raw = loadTemplate(templateName);
        String withoutOptionals = removeEmptyOptionalBlocks(raw, data);
        String withPlaceholders = replacePlaceholders(withoutOptionals, data);
        return stripClosingTags(withPlaceholders);
    }

    public TextVersions renderWithText(String templateName, AppointmentConfirmationEmailDTO data) {
        String html = render(templateName, data);
        return new TextVersions(html, stripHtml(html));
    }

    public record TextVersions(String html, String text) {}

    public MimeMessage createMessage(String to, String subject, String htmlContent, String textFallback) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textFallback != null ? textFallback : stripHtml(htmlContent), htmlContent);
            return message;
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Error creating email message", e);
        }
    }

    private String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, name -> {
            String path = "templates/email/" + name + ".html";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new RuntimeException("Email template not found: " + path);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Error loading template: " + path, e);
            }
        });
    }

    private String removeEmptyOptionalBlocks(String template, AppointmentConfirmationEmailDTO data) {
        return OPTIONAL_BLOCK_PATTERN.matcher(template).replaceAll(match -> {
            String varName = match.group(1);
            String value = getFieldValue(varName, data);
            if (value == null || value.isBlank() || "{{".equals(value)) {
                return "";
            }
            return match.group(0);
        });
    }

    private String replacePlaceholders(String template, AppointmentConfirmationEmailDTO data) {
        return PLACEHOLDER_PATTERN.matcher(template).replaceAll(match -> {
            String varName = match.group(1);
            String value = getFieldValue(varName, data);
            return value != null ? escapeHtml(value) : "";
        });
    }

    private String stripClosingTags(String template) {
        return CLOSING_TAG_PATTERN.matcher(template).replaceAll("");
    }

    private String getFieldValue(String fieldName, AppointmentConfirmationEmailDTO data) {
        String camelName = snakeToCamel(fieldName);
        String methodSuffix = Character.toUpperCase(camelName.charAt(0)) + camelName.substring(1);
        try {
            Method getter = data.getClass().getMethod("get" + methodSuffix);
            Object result = getter.invoke(data);
            return result != null ? result.toString() : "";
        } catch (NoSuchMethodException e) {
            return "";
        } catch (Exception e) {
            LOGGER.warn("Error reading field {} from DTO", fieldName, e);
            return "";
        }
    }

    private String snakeToCamel(String snakeCase) {
        Matcher m = Pattern.compile("_(.)").matcher(snakeCase);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(buf, Matcher.quoteReplacement(m.group(1).toUpperCase()));
        }
        m.appendTail(buf);
        return buf.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
