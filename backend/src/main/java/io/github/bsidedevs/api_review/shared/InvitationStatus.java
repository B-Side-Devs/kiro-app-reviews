package io.github.bsidedevs.api_review.shared;

/**
 * Lifecycle of a Project invitation. Transitions are one-way:
 * {@code PENDING → ACCEPTED | REVOKED | EXPIRED}. A revoked or expired
 * invitation cannot be re-accepted; a new one must be issued.
 */
public enum InvitationStatus {

    /** Issued but not yet accepted; does not grant access (Requirement 3.5). */
    PENDING,

    /** Grants Project access until revoked (Requirement 3.5). */
    ACCEPTED,

    /** Access revoked immediately; cannot be re-accepted (Requirement 11.5). */
    REVOKED,

    /** TTL elapsed without acceptance; never granted access. */
    EXPIRED
}
