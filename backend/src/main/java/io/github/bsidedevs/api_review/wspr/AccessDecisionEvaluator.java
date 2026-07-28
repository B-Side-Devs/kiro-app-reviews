package io.github.bsidedevs.api_review.wspr;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.AccessDecision;
import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.InvitationStatus;
import io.github.bsidedevs.api_review.shared.ProjectId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates access decisions for project authorization.
 *
 * <p>Contains the core authorization logic that checks role + project relationship
 * (the "double key" model). This class opens its own read-only transaction to
 * isolate authorization queries from the calling context.
 *
 * <p>Decision logic per role:
 * <ul>
 *   <li>{@code PROJECT_ADMIN}: granted if owner of the project, denied otherwise</li>
 *   <li>{@code CLIENT}: checks invitation status and project blocked state</li>
 *   <li>{@code PLATFORM_ADMIN}: always denied (no relation)</li>
 * </ul>
 */
@Component
class AccessDecisionEvaluator {

    private final ProjectRepository projectRepository;
    private final InvitationRepository invitationRepository;

    AccessDecisionEvaluator(ProjectRepository projectRepository,
                            InvitationRepository invitationRepository) {
        this.projectRepository = projectRepository;
        this.invitationRepository = invitationRepository;
    }

    /**
     * Evaluates whether the given principal has access to the specified project.
     *
     * @param principal the authenticated user; null produces DENIED_UNKNOWN
     * @param projectId the project to check; null produces DENIED_UNKNOWN
     * @return an AccessDecision indicating whether access is granted and the reason
     */
    @Transactional(readOnly = true)
    AccessDecision evaluateDecision(AuthenticatedPrincipal principal, ProjectId projectId) {
        if (principal == null || projectId == null) {
            return AccessDecision.denied(AccessReason.DENIED_UNKNOWN);
        }

        var projectOpt = projectRepository.findById(projectId.getValue());
        if (projectOpt.isEmpty()) {
            // Indistinguishable from not-found to avoid leaking existence
            return AccessDecision.denied(AccessReason.DENIED_UNKNOWN);
        }

        Project project = projectOpt.get();

        return switch (principal.role()) {
            case PROJECT_ADMIN -> evaluateProjectAdmin(principal, project);
            case CLIENT -> evaluateClient(principal, project);
            case PLATFORM_ADMIN -> AccessDecision.denied(AccessReason.DENIED_NO_RELATION);
        };
    }

    private AccessDecision evaluateProjectAdmin(AuthenticatedPrincipal principal, Project project) {
        if (project.isOwnedBy(principal.userId().getValue())) {
            return AccessDecision.granted(AccessReason.OWNER);
        }
        return AccessDecision.denied(AccessReason.DENIED_NO_RELATION);
    }

    private AccessDecision evaluateClient(AuthenticatedPrincipal principal, Project project) {
        List<Invitation> invitations = invitationRepository.findByProjectIdAndInviteeUserId(
                project.getProjectId(), principal.userId().getValue());

        if (invitations.isEmpty()) {
            return AccessDecision.denied(AccessReason.DENIED_NO_RELATION);
        }

        // Check if any invitation is ACCEPTED
        boolean hasAccepted = invitations.stream()
                .anyMatch(inv -> inv.getStatus() == InvitationStatus.ACCEPTED);

        if (hasAccepted) {
            // Requirement 5.13: blocked project denies access even with accepted invitation
            if (project.isBlocked()) {
                return AccessDecision.denied(AccessReason.DENIED_PROJECT_BLOCKED);
            }
            return AccessDecision.granted(AccessReason.ACCEPTED_INVITATION);
        }

        // Check if all invitations are PENDING
        boolean allPending = invitations.stream()
                .allMatch(inv -> inv.getStatus() == InvitationStatus.PENDING);

        if (allPending) {
            return AccessDecision.denied(AccessReason.DENIED_PENDING_INVITATION);
        }

        // Otherwise (REVOKED/EXPIRED mix without any ACCEPTED)
        return AccessDecision.denied(AccessReason.DENIED_REVOKED);
    }
}
