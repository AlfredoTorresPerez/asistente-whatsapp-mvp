package com.asistentewhatsapp.security.application;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditMetadata {

	private AuditMetadata() {
	}

	public static Map<String, Object> of(Object... keyValues) {
		if (keyValues == null || keyValues.length == 0) {
			return Map.of();
		}
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("Audit metadata requires key/value pairs.");
		}
		Map<String, Object> metadata = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			Object key = keyValues[i];
			Object value = keyValues[i + 1];
			if (key != null && value != null) {
				metadata.put(String.valueOf(key), value);
			}
		}
		return Map.copyOf(metadata);
	}
}
