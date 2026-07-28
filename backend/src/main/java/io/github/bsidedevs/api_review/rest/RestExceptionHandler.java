package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.observability.CorrelationContext;
import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.shared.DomainError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Global exception handler for the REST layer.
 *
 * <p>Converts Spring MVC and infrastructure exceptions into Problem+JSON responses
 * (RFC 7807). All handlers:
 * <ul>
 *   <li>Read the correlation ID from {@link CorrelationContext}</li>
 *   <li>Delegate response construction to {@link ProblemFactory}</li>
 *   <li>Return {@code Content-Type: application/problem+json}</li>
 * </ul>
 *
 * <p>Requirements covered: 10.5–10.10
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final ProblemFactory problemFactory;

    @SuppressWarnings("unused") // ObjectMapper kept for potential future serialisation needs
    private final ObjectMapper objectMapper;

    public RestExceptionHandler(ProblemFactory problemFactory, ObjectMapper objectMapper) {
        this.problemFactory = problemFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles bean-validation failures on {@code @RequestBody} parameters.
     * Returns 400 with field-level error details.
     * Requirements 10.5, 10.6
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReviewSessionDto.ProblemResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        String correlationId = CorrelationContext.get();

        List<ReviewSessionDto.FieldErrorDto> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ReviewSessionDto.FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ReviewSessionDto.ProblemResponse body = new ReviewSessionDto.ProblemResponse(
                "about:blank",
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                "/correlation/" + (correlationId != null ? correlationId : "unknown"),
                "VALIDATION_FAILED",
                null,
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    /**
     * Handles malformed or unreadable request bodies (e.g., invalid JSON syntax).
     * Returns 400 without exposing raw parse internals.
     * Requirements 10.7, 10.8
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReviewSessionDto.ProblemResponse> handleNotReadable(
            HttpMessageNotReadableException ex) {

        String correlationId = CorrelationContext.get();
        log.debug("Malformed request body [correlationId={}]: {}", correlationId, ex.getMessage());

        ReviewSessionDto.ProblemResponse body = problemFactory.buildFromCategory(
                DomainError.Category.VALIDATION,
                "MALFORMED_REQUEST",
                "Request body could not be read: " + ex.getMessage(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    /**
     * Handles type mismatch on path/query parameters (e.g., non-UUID where UUID expected).
     * Returns 400 with parameter name and rejected value in detail.
     * Requirements 10.8, 10.9
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ReviewSessionDto.ProblemResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String correlationId = CorrelationContext.get();
        String detail = String.format(
                "Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue());

        ReviewSessionDto.ProblemResponse body = problemFactory.buildFromCategory(
                DomainError.Category.VALIDATION,
                "INVALID_PARAMETER",
                detail,
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Returns 500 without exposing exception details to the client.
     * Requirements 10.10
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReviewSessionDto.ProblemResponse> handleUnexpected(Exception ex) {
        String correlationId = CorrelationContext.get();
        log.error("Unexpected error [correlationId={}]", correlationId, ex);

        ReviewSessionDto.ProblemResponse body = problemFactory.buildFromCategory(
                DomainError.Category.INTERNAL,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
