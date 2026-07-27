package io.github.bsidedevs.api_review.rest;

import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.shared.DomainError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for building Problem+JSON ({@link ReviewSessionDto.ProblemResponse}) responses
 * from {@link DomainError} instances or raw category/code/detail values.
 *
 * <p>Centralises HTTP status mapping and RFC 7807 response construction so that
 * both {@link ReviewSessionController} and {@link RestExceptionHandler} share the
 * same logic.
 */
@Component
public class ProblemFactory {

    /**
     * Maps a {@link DomainError.Category} to the corresponding {@link HttpStatus}.
     */
    public HttpStatus statusFor(DomainError.Category category) {
        return switch (category) {
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_AUTHORIZED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_TRANSITION -> HttpStatus.CONFLICT;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Builds a {@link ReviewSessionDto.ProblemResponse} from a {@link DomainError}.
     *
     * @param error         the domain error
     * @param correlationId the current request correlation ID (may be null)
     * @return a fully-populated problem response
     */
    public ReviewSessionDto.ProblemResponse build(DomainError error, String correlationId) {
        HttpStatus status = statusFor(error.category());
        String instance = "/correlation/" + (correlationId != null ? correlationId : "unknown");

        List<ReviewSessionDto.FieldErrorDto> fieldErrors = null;
        if (error.errors() != null) {
            fieldErrors = error.errors().stream()
                    .map(e -> new ReviewSessionDto.FieldErrorDto(e.field(), e.message()))
                    .toList();
        }

        return new ReviewSessionDto.ProblemResponse(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                error.message(),
                instance,
                error.code(),
                error.currentState() != null ? error.currentState().name() : null,
                fieldErrors
        );
    }

    /**
     * Overload for use when a full {@link DomainError} object is not available
     * (e.g., for uncaught infrastructure exceptions).
     *
     * @param category      error category to determine HTTP status
     * @param code          domain error code
     * @param detail        human-readable detail message
     * @param correlationId the current request correlation ID (may be null)
     * @return a problem response without field errors or current state
     */
    public ReviewSessionDto.ProblemResponse buildFromCategory(
            DomainError.Category category,
            String code,
            String detail,
            String correlationId) {

        HttpStatus status = statusFor(category);
        String instance = "/correlation/" + (correlationId != null ? correlationId : "unknown");

        return new ReviewSessionDto.ProblemResponse(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                instance,
                code,
                null,
                null
        );
    }

    /**
     * Builds a {@link ResponseEntity} wrapping the problem response for a {@link DomainError}.
     *
     * <p>Sets the HTTP status derived from the error category and the
     * {@code Content-Type: application/problem+json} header.
     *
     * @param error         the domain error
     * @param correlationId the current request correlation ID (may be null)
     * @return a {@code ResponseEntity} ready to return from a controller or advice
     */
    public ResponseEntity<ReviewSessionDto.ProblemResponse> toResponseEntity(
            DomainError error, String correlationId) {

        HttpStatus status = statusFor(error.category());
        ReviewSessionDto.ProblemResponse body = build(error, correlationId);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
