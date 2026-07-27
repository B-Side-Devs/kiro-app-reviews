package io.github.bsidedevs.api_review.iam;

import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;

import java.util.Objects;

/**
 * Authenticated principal representing the user making a request.
 *
 * <p>Contains the resolved identity (user ID and role) and the correlation ID
 * for tracing requests across system boundaries. This record is constructed
 * exclusively by the {@link PrincipalProvider} and passed explicitly to all
 * domain facade operations as the first parameter.
 *
 * <p>Role in the facade DAG: this is the input type for all authorization
 * checks. The {@code iam} domain does not depend on other domains; all other
 * domains may depend on {@code iam} for this type.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → section <em>Componente D: Proveedor_De_Principal</em>.
 *
 * @param userId the unique identifier of the authenticated user
 * @param role the role of the user (PROJECT_ADMIN, CLIENT, or PLATFORM_ADMIN)
 * @param correlationId the correlation ID for request tracing (raw string from header or generated value)
 */
public record AuthenticatedPrincipal(
        UserId userId,
        UserRole role,
        String correlationId) {

    /**
     * Compact constructor enforcing non-null invariants.
     * All three fields are required for a valid principal.
     */
    public AuthenticatedPrincipal {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
    }

    /**
     * Creates a new authenticated principal from raw values.
     *
     * @param userId the user identifier
     * @param role the user role
     * @param correlationId the correlation ID (raw string)
     * @return a new AuthenticatedPrincipal instance
     * @throws NullPointerException if any parameter is null
     */
    public static AuthenticatedPrincipal of(UserId userId, UserRole role, String correlationId) {
        return new AuthenticatedPrincipal(userId, role, correlationId);
    }
}
