package io.github.bsidedevs.api_review.shared;

/**
 * Persistence status of a Review Session's artifacts. Gates the
 * {@code RECORDING → COMPLETED} transition: it only succeeds when this
 * status is {@link #OK} (Requirement 10.2).
 *
 * @see ReviewSessionState
 */
public enum PersistenceStatus {

    /** All captured artifacts persisted successfully. */
    OK,

    /** One or more artifacts failed to persist; blocks completion until resolved. */
    FAILED,

    /** Persistence in progress (capture phase or completing transition). */
    IN_PROGRESS
}
