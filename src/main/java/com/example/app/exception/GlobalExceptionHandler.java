package com.example.app.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler – converts domain exceptions and security
 * errors into RFC 7807 Problem Detail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // 404 – resource not found
    // -----------------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 409 – duplicate resource (e.g., email already in use)
    // -----------------------------------------------------------------------
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleConflict(DuplicateResourceException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 400 – Bean Validation failures (@Valid on request bodies)
    // -----------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                (existing, replacement) -> existing
            ));

        log.warn("Validation failed: {}", fieldErrors);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation Failed");
        pd.setProperty("errors", fieldErrors);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 400 – Constraint violations (@Validated on controller method parameters)
    // -----------------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        log.warn("Constraint violation: {}", errors);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Parameter constraint validation failed");
        pd.setTitle("Constraint Violation");
        pd.setProperty("errors", errors);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 400 – HandlerMethodValidationException (Spring 6.1+ method param validation)
    // -----------------------------------------------------------------------
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Method parameter validation failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request parameter validation failed");
        pd.setTitle("Validation Failed");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 401 – unauthorized (custom + Spring Security AuthenticationException)
    // -----------------------------------------------------------------------
    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ProblemDetail handleUnauthorized(Exception ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Unauthorized");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 403 – access denied (Spring Security AccessDeniedException)
    // -----------------------------------------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, "Access is denied: You do not have permission to access this resource");
        pd.setTitle("Forbidden");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 400 – illegal argument / business rule validation
    // -----------------------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Bad Request");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 400 – malformed JSON or unreadable message
    // -----------------------------------------------------------------------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid parameter value");
        pd.setTitle("Bad Request");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 409 – database constraint / duplicate data integrity violation
    // -----------------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Database constraint violation: resource already exists");
        pd.setTitle("Conflict");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // -----------------------------------------------------------------------
    // 500 – catch-all
    // -----------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
