package io.github.bsidedevs.api_review.shared;

/**
 * Decision of an authorization check, indicating whether access is granted
 * and the reason for that decision.
 *
 * <p>Used by the {@link wspr.WorkspaceProjectFacade} to communicate the result
 * of the dual-key authorization check (role + project relationship) to domain
 * operations in {@code rs} and other modules.
 *
 * <p>The {@link #reason} field explains why access was granted or denied. The
 * {@code DENIED_*} reasons are prioritized according to the fail-closed rule:
 * any denial reason wins over a grant reason.
 *
 * @param granted true if access is granted, false if access is denied
 * @param reason the reason explaining the decision (OWNER, ACCEPTED_INVITATION,
 *               ROLE_ADMIN, or one of the DENIED_* reasons)
 * @see AccessReason
 * @see wspr.WorkspaceProjectFacade
 */
public record AccessDecision(boolean granted, AccessReason reason) {

    /**
     * Creates an access decision indicating access was granted.
     *
     * @param reason the reason for granting access (OWNER, ACCEPTED_INVITATION, or ROLE_ADMIN)
     * @return a new AccessDecision with granted=true
     * @throws NullPointerException if reason is null
     */
    public static AccessDecision granted(AccessReason reason) {
        return new AccessDecision(true, reason);
    }

    /**
     * Creates an access decision indicating access was denied.
     *
     * @param reason the reason for denying access (one of the DENIED_* reasons)
     * @return a new AccessDecision with granted=false
     * @throws NullPointerException if reason is null
     */
    public static AccessDecision denied(AccessReason reason) {
        return new AccessDecision(false, reason);
    }
}
