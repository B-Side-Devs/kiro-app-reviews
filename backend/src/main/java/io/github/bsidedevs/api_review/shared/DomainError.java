package io.github.bsidedevs.api_review.shared;

import java.util.List;

/**
 * Error type returned by Review Session facade operations.
 *
 * <p> Six categories aligning with the fail-closed authorization model:
 * - {@link #UNAUTHENTICATED}: missing or invalid principal (HTTP 401)
 * - {@link #NOT_AUTHORIZED}: authorized but without required permission (HTTP 403)
 * - {@link #NOT_FOUND}: resource not found or access denied (HTTP 404)
 * - {@link #INVALID_TRANSITION}: state transition not allowed (HTTP 409)
 * - {@link #VALIDATION}: input validation failed (HTTP 400)
 * - {@link #INTERNAL}: unexpected error (HTTP 500)
 *
 * <p>Each error has a {@link #category} for HTTP mapping and an optional
 * {@link #code} for more specific error classification.
 *
 * <p>For {@link #INVALID_TRANSITION}, {@link #currentState} indicates the
 * current state when the transition was attempted. For {@link #VALIDATION},
 * {@link #errors} contains field-level validation errors.
 *
 * @param category error category for HTTP status mapping
 * @param code specific error code for client handling
 * @param currentState current state when transition failed (optional)
 * @param errors field validation errors (optional)
 */
public record DomainError(
        Category category,
        String code,
        String message,
        ReviewSessionState currentState,
        List<FieldError> errors
) {

    /**
     * Creates an UNAUTHENTICATED error.
     *
     * @param code specific error code
     * @param message error message
     * @return domain error
     */
    public static DomainError unauthenticated(String code, String message) {
        return new DomainError(Category.UNAUTHENTICATED, code, message, null, null);
    }

    /**
     * Creates a NOT_AUTHORIZED error.
     *
     * @param code specific error code
     * @param message error message
     * @return domain error
     */
    public static DomainError notAuthorized(String code, String message) {
        return new DomainError(Category.NOT_AUTHORIZED, code, message, null, null);
    }

    /**
     * Creates a NOT_FOUND error.
     *
     * @param code specific error code
     * @param message error message
     * @return domain error
     */
    public static DomainError notFound(String code, String message) {
        return new DomainError(Category.NOT_FOUND, code, message, null, null);
    }

    /**
     * Creates an INVALID_TRANSITION error.
     *
     * @param code specific error code
     * @param message error message
     * @param currentState current state when transition failed
     * @return domain error
     */
    public static DomainError invalidTransition(String code, String message, ReviewSessionState currentState) {
        return new DomainError(Category.INVALID_TRANSITION, code, message, currentState, null);
    }

    /**
     * Creates a VALIDATION error.
     *
     * @param code specific error code
     * @param message error message
     * @param errors field-level validation errors
     * @return domain error
     */
    public static DomainError validation(String code, String message, List<FieldError> errors) {
        return new DomainError(Category.VALIDATION, code, message, null, errors);
    }

    /**
     * Creates an INTERNAL error.
     *
     * @param code specific error code
     * @param message error message
     * @return domain error
     */
    public static DomainError internal(String code, String message) {
        return new DomainError(Category.INTERNAL, code, message, null, null);
    }

    /**
     * Error categories for HTTP status mapping.
     *
     * <p>- UNAUTHENTICATED → 401
     * - NOT_AUTHORIZED → 403
     * - NOT_FOUND → 404
     * - INVALID_TRANSITION → 409
     * - VALIDATION → 400
     * - INTERNAL → 500
     */
    public enum Category {
        UNAUTHENTICATED,
        NOT_AUTHORIZED,
        NOT_FOUND,
        INVALID_TRANSITION,
        VALIDATION,
        INTERNAL
    }

    /**
     * Field-level validation error.
     *
     * @param field field name
     * @param message error message
     */
    public record FieldError(String field, String message) {
    }
}
