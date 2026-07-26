package io.github.bsidedevs.api_review.shared;

/**
 * Reason attached to an authorization {@code AccessDecision} explaining why
 * access was granted or denied. Fail-closed: any denial reason wins over a
 * grant reason (Requirements 9.6, 11.4, 18.7).
 *
 * @see AccessError
 */
public enum AccessReason {

    /** Project Admin accessing a Project they own. */
    OWNER,

    /** Client with an accepted invitation to the Project. */
    ACCEPTED_INVITATION,

    /** Reserved for administrative/backoffice access; never granted for session content. */
    ROLE_ADMIN,

    /** Neither ownership nor any invitation exists for this user/Project pair. */
    DENIED_NO_RELATION,

    /** User identity is inactive or has no recognized role. */
    DENIED_NO_ROLE,

    /** Invitation exists but has not been accepted yet (Requirement 3.5). */
    DENIED_PENDING_INVITATION,

    /** Invitation was revoked; access ends immediately (Requirement 11.5). */
    DENIED_REVOKED,

    /** Project is in a blocked state restricting Client access (Requirement 5.7). */
    DENIED_PROJECT_BLOCKED,

    /** Resource not found or an unexpected error occurred; avoids leaking existence. */
    DENIED_UNKNOWN
}
