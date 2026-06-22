package com.asistentewhatsapp.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message);
    }

    public AuthenticationFailedException(String message, Map<String, String> fieldErrors) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message, fieldErrors);
    }
}

