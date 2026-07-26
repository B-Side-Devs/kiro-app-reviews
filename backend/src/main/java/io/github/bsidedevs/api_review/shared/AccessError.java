package io.github.bsidedevs.api_review.shared;

/**
 * Access error categories returned by authorization checks and propagated
 * through {@link Result}. Supports fail-closed authorization: any denial
 * takes precedence over any concurrent grant (Requirements 9.6, 11.4, 18.7).
 *
 * <p>HTTP mapping: {@link #UNAUTHENTICATED} → 401, {@link #FORBIDDEN} → 403,
 * {@link #NOT_FOUND} → 404 (used to avoid revealing existence).
 *
 * @see Result
 * @see AccessReason
 */
public enum AccessError {
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND
}
