package io.github.bsidedevs.api_review.shared;

/**
 * User roles in the Reviews platform. A user has exactly one role for its
 * lifetime; role changes are out of scope for the MVP.
 *
 * <p>Authorization always combines role with the user's relationship to the
 * target resource (the "double key" model) — never role alone.
 */
public enum UserRole {

    /** Owns Workspaces/Projects; manages Client invitations and Review Session lifecycle. */
    PROJECT_ADMIN,

    /** Participates in Review Sessions of Projects where it holds an accepted invitation. */
    CLIENT,

    /** Backoffice access only; explicitly excluded from Review Session content (Req. 6.5, 6.6, 11.3). */
    PLATFORM_ADMIN
}
