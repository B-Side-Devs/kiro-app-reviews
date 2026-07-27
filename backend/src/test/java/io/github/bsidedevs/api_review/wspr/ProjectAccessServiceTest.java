package io.github.bsidedevs.api_review.wspr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.AccessDecision;
import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProjectAccessService}.
 * Validates Requirement 5.6 (timeout) and error handling behavior.
 */
class ProjectAccessServiceTest {

    private AccessDecisionEvaluator evaluator;
    private ProjectAccessService service;

    private static final UUID PROJECT_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000001");
    private static final UUID USER_UUID = UUID.fromString("01903d6a-1234-7000-8000-000000000002");

    @BeforeEach
    void setUp() {
        evaluator = mock(AccessDecisionEvaluator.class);
        service = new ProjectAccessService(evaluator);
    }

    private AuthenticatedPrincipal principal() {
        return AuthenticatedPrincipal.of(UserId.of(USER_UUID), UserRole.PROJECT_ADMIN, "corr-id");
    }

    @Test
    @DisplayName("delegates to evaluator and returns result on success")
    void delegatesToEvaluator() {
        ProjectId projectId = ProjectId.of(PROJECT_UUID);
        AccessDecision expected = AccessDecision.granted(AccessReason.OWNER);
        when(evaluator.evaluateDecision(any(), any())).thenReturn(expected);

        AccessDecision result = service.canAccessProject(principal(), projectId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns DENIED_UNKNOWN when evaluator throws exception")
    void handlesEvaluatorException() {
        ProjectId projectId = ProjectId.of(PROJECT_UUID);
        when(evaluator.evaluateDecision(any(), any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        AccessDecision result = service.canAccessProject(principal(), projectId);

        assertThat(result.granted()).isFalse();
        assertThat(result.reason()).isEqualTo(AccessReason.DENIED_UNKNOWN);
    }

    @Test
    @DisplayName("returns DENIED_UNKNOWN on timeout (Requirement 5.6)")
    void handlesTimeout() {
        ProjectId projectId = ProjectId.of(PROJECT_UUID);
        when(evaluator.evaluateDecision(any(), any())).thenAnswer(invocation -> {
            Thread.sleep(3000); // exceeds 2000ms budget
            return AccessDecision.granted(AccessReason.OWNER);
        });

        AccessDecision result = service.canAccessProject(principal(), projectId);

        assertThat(result.granted()).isFalse();
        assertThat(result.reason()).isEqualTo(AccessReason.DENIED_UNKNOWN);
    }
}
