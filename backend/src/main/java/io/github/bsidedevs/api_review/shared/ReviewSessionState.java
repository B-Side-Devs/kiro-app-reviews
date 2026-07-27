package io.github.bsidedevs.api_review.shared;

/**
 * Lifecycle states of a Review Session.
 *
 * <p>Valid transitions: {@code DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED
 * → DELETED}. {@link #DELETED} is terminal and absorbing (Requirement 8.11).
 *
 * <p>Operations enabled per state:
 * <ul>
 *   <li>{@link #DRAFT} — no capture/annotation yet; visible in active listings.</li>
 *   <li>{@link #RECORDING} — capture and annotations enabled.</li>
 *   <li>{@link #COMPLETED} / {@link #REOPENED} — annotations only, no capture.</li>
 *   <li>{@link #ARCHIVED} — read-only, excluded from active listings.</li>
 *   <li>{@link #DELETED} — terminal, all operations blocked.</li>
 * </ul>
 *
 * @see PersistenceStatus
 */
public enum ReviewSessionState {

    /** Initial state on creation (Requirement 8.2). Can move to RECORDING, ARCHIVED or DELETED. */
    DRAFT,

    /** Active capture. Moves to COMPLETED once persistence succeeds (Requirement 8.8). */
    RECORDING,

    /** Capture finished. Can be reopened, archived or deleted by the owning Project Admin. */
    COMPLETED,

    /** Reopened for further annotation; can return to COMPLETED, be archived or deleted. */
    REOPENED,

    /** Read-only; content stays queryable but no further modification is allowed. */
    ARCHIVED,

    /** Terminal, absorbing state (Requirement 8.11). No outgoing transitions. */
    DELETED
}
