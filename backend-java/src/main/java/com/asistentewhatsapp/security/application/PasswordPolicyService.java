package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.domain.SecurityPolicyEntity;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");

    public void validateNewPassword(String newPassword, SecurityPolicyEntity securityPolicy) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        if (newPassword.length() < securityPolicy.getPasswordMinLength()) {
            fieldErrors.put(
                    "newPassword",
                    "La nueva contrasena debe tener al menos %d caracteres."
                            .formatted(securityPolicy.getPasswordMinLength()));
        } else if (newPassword.length() > 72) {
            fieldErrors.put("newPassword", "La nueva contrasena no puede superar 72 caracteres.");
        } else if (securityPolicy.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(newPassword).matches()) {
            fieldErrors.put("newPassword", "La nueva contrasena debe incluir al menos una mayuscula.");
        } else if (securityPolicy.isRequireNumber() && !NUMBER_PATTERN.matcher(newPassword).matches()) {
            fieldErrors.put("newPassword", "La nueva contrasena debe incluir al menos un numero.");
        } else if (securityPolicy.isRequireSymbol() && !SYMBOL_PATTERN.matcher(newPassword).matches()) {
            fieldErrors.put("newPassword", "La nueva contrasena debe incluir al menos un simbolo.");
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La solicitud contiene datos invalidos.",
                    fieldErrors);
        }
    }
}

