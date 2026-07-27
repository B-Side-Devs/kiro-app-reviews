package io.github.bsidedevs.api_review.observability;

import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Security audit logger for recording unauthorized access attempts to a dedicated
 * security log channel, separate from the main operational log.
 *
 * <p>All methods suppress exceptions internally — a failure to write a security
 * audit event must never propagate to callers, must not change the HTTP error
 * category returned to the requester, and must not cause the denied operation
 * to be executed (Requirement 5.14).
 *
 * <p>Log destination: SLF4J logger named {@code "security"}, which can be routed
 * independently in {@code logback-spring.xml} (e.g., to a dedicated appender or
 * a SIEM-forwarding channel).
 *
 * <p>See {@code design.md} → section <em>Componente E: SecurityAuditLogger</em>.
 */
@Component
public class SecurityAuditLogger {

    /** Dedicated security logger, separate from the operational log. */
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("security");

    /**
     * Records an unauthorized access attempt after an access decision has been denied.
     *
     * <p>Logs at WARN level with the requesting user's ID, the resource that was
     * requested, the denial reason, and the correlation ID of the request
     * (Requirement 5.8).
     *
     * <p>Any exception thrown internally is suppressed; the denial outcome is
     * preserved regardless of logging success (Requirement 5.14).
     *
     * @param userId        the identifier of the requester (never null under normal flow)
     * @param resourceId    the identifier of the requested resource (e.g. ReviewSessionId)
     * @param reason        the {@link AccessReason} explaining why access was denied
     * @param correlationId the correlation ID of the originating request
     */
    public void denied(UserId userId, Object resourceId, AccessReason reason, String correlationId) {
        try {
            SECURITY_LOG.warn(
                    "ACCESS_DENIED userId={} resourceId={} reason={} correlationId={}",
                    userId, resourceId, reason, correlationId);
        } catch (Exception ex) {
            // Suppress: logging failure must never propagate (Requirement 5.14)
        }
    }

    /**
     * Records an unauthenticated request — one that arrived without a recognisable
     * identity header, or with one that could not be resolved to an active user.
     *
     * <p>Logs at WARN level. The {@code rawHeaderPresent} flag distinguishes between
     * a completely missing header and one that was present but invalid/unresolvable,
     * which can be useful for detecting misconfigured clients vs. probing attempts.
     *
     * <p>Any exception thrown internally is suppressed (Requirement 5.14).
     *
     * @param rawHeaderPresent {@code true} if the identity header was present in the
     *                         request but could not be resolved; {@code false} if it
     *                         was entirely absent
     * @param correlationId    the correlation ID of the originating request
     */
    public void unauthenticated(boolean rawHeaderPresent, String correlationId) {
        try {
            SECURITY_LOG.warn(
                    "UNAUTHENTICATED_ACCESS rawHeaderPresent={} correlationId={}",
                    rawHeaderPresent, correlationId);
        } catch (Exception ex) {
            // Suppress: logging failure must never propagate (Requirement 5.14)
        }
    }
}
