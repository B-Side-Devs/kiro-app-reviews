/**
 * Cross-cutting package {@code security} (authentication / authorization filters,
 * credential hashing).
 *
 * <p>Responsibility: hosts the Spring Security authentication filter (resolves
 * {@code AuthenticatedPrincipal} by delegating to the {@code iam} facade), the global
 * fail-closed authorization filter (delegates to the {@code wspr} facade
 * {@code canAccessProject}), and the credential-hashing configuration (bcrypt / argon2
 * — concrete choice deferred to ADR-003). Requests without a valid credential return
 * {@code 401} before reaching any domain handler; any {@code DENY} result short-circuits
 * with {@code 403} (or {@code 404} where existence should not be revealed).
 *
 * <p>Role in the facade DAG: <strong>cross-cutting infrastructure</strong>. Depends on
 * the public facades of {@code iam} and {@code wspr}; must not access domain internal
 * classes. Does not appear inside the domain DAG itself.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}, {@code observability} (to log
 * security-audit events with correlation).
 *
 * <p>See {@code design.md} → sections about authentication and authorization filters
 * (fail-closed).
 */
package io.github.bsidedevs.api_review.security;
