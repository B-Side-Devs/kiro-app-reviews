package io.github.bsidedevs.api_review.wspr;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.AccessDecision;
import io.github.bsidedevs.api_review.shared.ProjectId;

/**
 * Facade for workspace-project authorization queries.
 *
 * <p>Responsibility: única fuente de verdad de la relación usuario–Proyecto. Exposes
 * the {@code canAccessProject} operation that evaluates dual-key authorization
 * (role + project relationship) and returns an {@link AccessDecision} with a
 * {@link AccessReason}.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code iam} (to resolve {@code AuthenticatedPrincipal}), {@code shared} (for types).</li>
 *   <li>Depended upon by: {@code rs} (ReviewSessionService).</li>
 * </ul>
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → section <em>Componente C: Fachada_WSPR mínima</em>.
 */
public interface WorkspaceProjectFacade {

    /**
     * Evaluates whether the given principal has access to the specified project.
     *
     * <p>This operation is invoked by the Review Session facade before any state
     * evaluation or resource lookup (Requirement 5.1). The result determines
     * authorization and is never cached per request.
     *
     * <p>Fal-closed: if the decision cannot be computed within the budget
     * (e.g., timeout, repository failure), returns a denied decision with
     * {@link AccessReason#DENIED_UNKNOWN}.
     *
     * @param principal the authenticated user making the request; must not be null
     * @param projectId the project identifier to check access for; must not be null
     * @return an AccessDecision indicating whether access is granted and the reason
     */
    AccessDecision canAccessProject(AuthenticatedPrincipal principal, ProjectId projectId);
}
