/**
 * Cross-cutting package {@code security} (auth filters, credential hashing).
 * Not implemented yet — scaffolding only.
 *
 * <p>Responsibility: will host the authentication filter (resolves
 * {@code AuthenticatedPrincipal} via the {@code iam} facade), the global fail-closed
 * authorization filter (via {@code wspr.canAccessProject}), and the credential-hashing
 * configuration (bcrypt / argon2, concrete choice deferred to ADR-003). Requests
 * without valid credentials will return {@code 401} before reaching any handler; any
 * {@code DENY} short-circuits with {@code 403} (or {@code 404} to avoid revealing
 * existence).
 *
 * <p>Role in the facade DAG: <strong>cross-cutting infrastructure</strong>. Depends on
 * the public facades of {@code iam} and {@code wspr}; does not appear in the domain DAG.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}, {@code observability}.
 *
 * <p>See {@code design.md} → sections about authentication and authorization filters
 * (fail-closed).
 */
package io.github.bsidedevs.api_review.security;
