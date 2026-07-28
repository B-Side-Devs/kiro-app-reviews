package io.github.bsidedevs.api_review.wspr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.AccessDecision;
import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.InvitationStatus;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AccessDecisionEvaluator}.
 * Validates Requirements 5.2-5.6, 5.11-5.13.
 */
class AccessDecisionEvaluatorTest {

    private ProjectRepository projectRepository;
    private InvitationRepository invitationRepository;
    private AccessDecisionEvaluator evaluator;

    // Reusable test UUIDs (UUID v7-like)
    private static final UUID PROJECT_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000001");
    private static final UUID OWNER_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000002");
    private static final UUID CLIENT_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000003");
    private static final UUID PLATFORM_ADMIN_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000004");
    private static final UUID NON_OWNER_ADMIN_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000005");
    private static final UUID WORKSPACE_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000006");
    private static final UUID INVITATION_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000007");

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        invitationRepository = mock(InvitationRepository.class);
        evaluator = new AccessDecisionEvaluator(projectRepository, invitationRepository);
    }

    private AuthenticatedPrincipal principal(UUID userId, UserRole role) {
        return AuthenticatedPrincipal.of(UserId.of(userId), role, "test-correlation-id");
    }

    private Project newProject(boolean blocked) {
        Project project = new Project(PROJECT_UUID, WORKSPACE_UUID, OWNER_UUID, "Test Project");
        if (blocked) {
            // Use reflection to set blocked since there's no setter
            try {
                var field = Project.class.getDeclaredField("blocked");
                field.setAccessible(true);
                field.set(project, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return project;
    }

    private Invitation newInvitation(InvitationStatus status) {
        return new Invitation(INVITATION_UUID, PROJECT_UUID, CLIENT_UUID, status, Instant.now());
    }

    @Nested
    @DisplayName("Null input handling")
    class NullInputTests {

        @Test
        @DisplayName("null principal returns DENIED_UNKNOWN")
        void nullPrincipal() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            AccessDecision decision = evaluator.evaluateDecision(null, projectId);
            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_UNKNOWN);
        }

        @Test
        @DisplayName("null projectId returns DENIED_UNKNOWN")
        void nullProjectId() {
            AuthenticatedPrincipal p = principal(OWNER_UUID, UserRole.PROJECT_ADMIN);
            AccessDecision decision = evaluator.evaluateDecision(p, null);
            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Project not found")
    class ProjectNotFoundTests {

        @Test
        @DisplayName("non-existent project returns DENIED_UNKNOWN")
        void projectNotFound() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.empty());

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(OWNER_UUID, UserRole.PROJECT_ADMIN), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_UNKNOWN);
        }
    }

    @Nested
    @DisplayName("PROJECT_ADMIN role (Requirement 5.2)")
    class ProjectAdminTests {

        @Test
        @DisplayName("owner gets granted with OWNER reason")
        void ownerGranted() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(OWNER_UUID, UserRole.PROJECT_ADMIN), projectId);

            assertThat(decision.granted()).isTrue();
            assertThat(decision.reason()).isEqualTo(AccessReason.OWNER);
        }

        @Test
        @DisplayName("non-owner PROJECT_ADMIN gets DENIED_NO_RELATION")
        void nonOwnerDenied() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(NON_OWNER_ADMIN_UUID, UserRole.PROJECT_ADMIN), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_NO_RELATION);
        }

        @Test
        @DisplayName("owner access granted regardless of blocked status (Requirement 5.4)")
        void ownerGrantedEvenIfBlocked() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(true)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(OWNER_UUID, UserRole.PROJECT_ADMIN), projectId);

            assertThat(decision.granted()).isTrue();
            assertThat(decision.reason()).isEqualTo(AccessReason.OWNER);
        }
    }

    @Nested
    @DisplayName("CLIENT role (Requirements 5.3, 5.11-5.13)")
    class ClientTests {

        @Test
        @DisplayName("no invitations returns DENIED_NO_RELATION")
        void noInvitations() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(Collections.emptyList());

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_NO_RELATION);
        }

        @Test
        @DisplayName("accepted invitation on non-blocked project returns ACCEPTED_INVITATION (Req 5.3)")
        void acceptedInvitationGranted() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(List.of(newInvitation(InvitationStatus.ACCEPTED)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isTrue();
            assertThat(decision.reason()).isEqualTo(AccessReason.ACCEPTED_INVITATION);
        }

        @Test
        @DisplayName("accepted invitation on blocked project returns DENIED_PROJECT_BLOCKED (Req 5.13)")
        void acceptedInvitationBlockedProject() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(true)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(List.of(newInvitation(InvitationStatus.ACCEPTED)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_PROJECT_BLOCKED);
        }

        @Test
        @DisplayName("all pending invitations returns DENIED_PENDING_INVITATION (Req 5.11)")
        void allPendingInvitations() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(List.of(newInvitation(InvitationStatus.PENDING)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_PENDING_INVITATION);
        }

        @Test
        @DisplayName("revoked invitation returns DENIED_REVOKED (Req 5.12)")
        void revokedInvitation() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(List.of(newInvitation(InvitationStatus.REVOKED)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_REVOKED);
        }

        @Test
        @DisplayName("expired invitation returns DENIED_REVOKED (Req 5.12)")
        void expiredInvitation() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));
            when(invitationRepository.findByProjectIdAndInviteeUserId(PROJECT_UUID, CLIENT_UUID))
                    .thenReturn(List.of(newInvitation(InvitationStatus.EXPIRED)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(CLIENT_UUID, UserRole.CLIENT), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_REVOKED);
        }
    }

    @Nested
    @DisplayName("PLATFORM_ADMIN role (Requirement 5.5)")
    class PlatformAdminTests {

        @Test
        @DisplayName("PLATFORM_ADMIN always denied with DENIED_NO_RELATION")
        void platformAdminDenied() {
            ProjectId projectId = ProjectId.of(PROJECT_UUID);
            when(projectRepository.findById(PROJECT_UUID)).thenReturn(Optional.of(newProject(false)));

            AccessDecision decision = evaluator.evaluateDecision(
                    principal(PLATFORM_ADMIN_UUID, UserRole.PLATFORM_ADMIN), projectId);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessReason.DENIED_NO_RELATION);
        }
    }
}
