package com.asistentewhatsapp.shared.exception;

import com.asistentewhatsapp.shared.api.ApiErrorResponse;
import com.asistentewhatsapp.shared.infrastructure.SlackNotifier;
import com.asistentewhatsapp.shared.observability.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired(required = false)
    private SlackNotifier slackNotifier;

    public GlobalExceptionHandler() {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "La solicitud contiene datos invalidos.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> violations = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "La solicitud contiene restricciones invalidas.",
                request,
                violations);
    }

    @ExceptionHandler(UnsupportedMessagingChannelException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMessagingChannel(
            UnsupportedMessagingChannelException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CHANNEL_NOT_CONFIGURED",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(MessagingChannelUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleChannelUnavailable(
            MessagingChannelUnavailableException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "CHANNEL_UNAVAILABLE",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        return buildResponse(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                request,
                exception.getFieldErrors());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Debes iniciar sesion para continuar.",
                request,
                Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "No tienes permisos para realizar esta accion.",
                request,
                Map.of());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateKeyException(
            DuplicateKeyException exception,
            HttpServletRequest request) {
        String message = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();
        boolean isBookingDuplicate = message != null
                && message.contains("uq_booking_customer_professional_active");
        if (isBookingDuplicate) {
            LOGGER.warn("Intento de reserva duplicada: {}", message);
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "BOOKING_DUPLICATE",
                    "Ya existe una reserva activa para este cliente, profesional y horario.",
                    request,
                    Map.of());
        }
        return handleUnexpectedException(exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Excepcion no manejada: ", exception);
        if (slackNotifier != null) slackNotifier.notifyError(
                "500 en " + request.getMethod() + " " + request.getRequestURI(),
                exception.toString());
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrio un error inesperado. Intenta nuevamente.",
                request,
                Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        String correlationId = CorrelationIdFilter.currentCorrelationId();
        if (status.is5xxServerError()) {
            LOGGER.error(
                    "[Backend - capa de servidor] Respuesta de error codigo={} estado={} ruta={} correlationId={} mensaje={}",
                    code,
                    status.value(),
                    request.getRequestURI(),
                    correlationId,
                    message);
        } else if (status.is4xxClientError()) {
            LOGGER.warn(
                    "[Backend - capa de servidor] Respuesta de advertencia codigo={} estado={} ruta={} correlationId={} mensaje={}",
                    code,
                    status.value(),
                    request.getRequestURI(),
                    correlationId,
                    message);
        }

        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                correlationId,
                fieldErrors));
    }
}
