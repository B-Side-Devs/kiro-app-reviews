package io.github.bsidedevs.api_review.iam;

import io.github.bsidedevs.api_review.shared.AccessError;
import io.github.bsidedevs.api_review.shared.Result;

import java.util.List;

/**
 * Provider for resolving the authenticated principal from external signals.
 *
 * <p>Sustituible: the current implementation (HeaderPrincipalProvider) resolves
 * the principal from HTTP headers during development. The production authentication
 * filter (Etapa 2) will replace this implementation with a Spring Security integration
 * without changing the contract or its consumers.
 *
 * <p>Role in the facade DAG: this is the <strong>root</strong> of the identity
 * resolution chain. The {@code iam} domain does not depend on other domains;
 * all other domains may depend on {@code iam} for this type.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → section <em>Componente D: Proveedor_De_Principal</em>.
 */
public interface PrincipalProvider {

    /**
     * Resolves the authenticated principal from the provided header values and correlation ID.
     *
     * <p>Fail-closed: if the principal cannot be resolved (missing header, multiple headers,
     * invalid user ID, user not found, repository failure), returns an error with
     * category {@link AccessError#UNAUTHENTICATED}.
     *
     * @param headerValues all occurrences of the identity header received in the request
     * @param correlationId the correlation ID for tracing
     * @return an AuthenticatedPrincipal if resolved successfully, or an error if resolution failed
     */
    Result<AuthenticatedPrincipal, AccessError> resolve(List<String> headerValues, String correlationId);
}
