package com.asistentewhatsapp.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public ConflictException(String message, Map<String, String> fieldErrors) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, fieldErrors);
    }
}
