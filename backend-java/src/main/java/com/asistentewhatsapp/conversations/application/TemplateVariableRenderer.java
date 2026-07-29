package com.asistentewhatsapp.conversations.application;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplateVariableRenderer {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

	public String render(String templateBody, Map<String, String> variables) {
		Matcher matcher = VARIABLE_PATTERN.matcher(templateBody);
		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String variableName = matcher.group(1);
			String replacement = variables.getOrDefault(variableName, "");
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
		}

		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
