package io.github.bsidedevs.api_review.art;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bsidedevs.api_review.art.SceneFacade.SceneView;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.ReviewSessionView;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SceneService}.
 *
 * <p>Validates the MVP domain rules: authorization/state resolution is delegated
 * to the {@code rs} facade, saves are allowed only while RECORDING, saves are
 * upserts, and retrieval returns NOT_FOUND when no Scene exists.
 */
class SceneServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String DOC = "{\"type\":\"excalidraw\",\"version\":2,\"elements\":[]}";
    private static final String NEW_DOC = "{\"type\":\"excalidraw\",\"version\":2,\"elements\":[{\"id\":\"a\"}]}";

    private final ReviewSessionId sessionId = ReviewSessionId.generate();

    private ReviewSessionFacade reviewSessionFacade;
    private SceneRepository sceneRepository;
    private SceneService service;

    @BeforeEach
    void setUp() {
        reviewSessionFacade = mock(ReviewSessionFacade.class);
        sceneRepository = mock(SceneRepository.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new SceneService(reviewSessionFacade, sceneRepository, clock);
    }

    private AuthenticatedPrincipal principal() {
        return AuthenticatedPrincipal.of(UserId.of(sessionId.getValue()), UserRole.PROJECT_ADMIN, "corr");
    }

    private void stubSessionState(ReviewSessionState state) {
        ReviewSessionView view = new ReviewSessionView(
                sessionId,
                ProjectId.of(sessionId.getValue()),
                UserId.of(sessionId.getValue()),
                state,
                PersistenceStatus.OK,
                Optional.empty(), Optional.empty(), Optional.empty(),
                NOW,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        when(reviewSessionFacade.getReviewSession(any(), any())).thenReturn(Result.ok(view));
    }

    @Test
    @DisplayName("save creates a new Scene when session is RECORDING and none exists")
    void saveCreatesWhenRecording() {
        stubSessionState(ReviewSessionState.RECORDING);
        when(sceneRepository.findById(sessionId.getValue())).thenReturn(Optional.empty());
        when(sceneRepository.save(any(Scene.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<SceneView, DomainError> result = service.saveScene(principal(), sessionId, DOC);

        assertThat(result.isOk()).isTrue();
        assertThat(result.unwrap().document()).isEqualTo(DOC);
        assertThat(result.unwrap().createdAt()).isEqualTo(NOW);
        assertThat(result.unwrap().updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("save replaces the existing Scene document (upsert)")
    void saveReplacesExisting() {
        stubSessionState(ReviewSessionState.RECORDING);
        Instant earlier = NOW.minusSeconds(3600);
        Scene existing = new Scene(sessionId.getValue(), DOC, earlier);
        when(sceneRepository.findById(sessionId.getValue())).thenReturn(Optional.of(existing));
        when(sceneRepository.save(any(Scene.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<SceneView, DomainError> result = service.saveScene(principal(), sessionId, NEW_DOC);

        assertThat(result.isOk()).isTrue();
        assertThat(result.unwrap().document()).isEqualTo(NEW_DOC);
        assertThat(result.unwrap().createdAt()).isEqualTo(earlier); // preserved
        assertThat(result.unwrap().updatedAt()).isEqualTo(NOW);      // refreshed
    }

    @Test
    @DisplayName("save is rejected with SCENE_NOT_EDITABLE when session is not RECORDING")
    void saveRejectedWhenNotRecording() {
        stubSessionState(ReviewSessionState.COMPLETED);

        Result<SceneView, DomainError> result = service.saveScene(principal(), sessionId, DOC);

        assertThat(result.isErr()).isTrue();
        assertThat(result.unwrapErr().category()).isEqualTo(DomainError.Category.INVALID_TRANSITION);
        assertThat(result.unwrapErr().code()).isEqualTo("SCENE_NOT_EDITABLE");
        assertThat(result.unwrapErr().currentState()).isEqualTo(ReviewSessionState.COMPLETED);
    }

    @Test
    @DisplayName("save propagates the rs facade error (e.g. not found / access denied)")
    void savePropagatesSessionError() {
        DomainError notFound = DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found");
        when(reviewSessionFacade.getReviewSession(any(), any())).thenReturn(Result.err(notFound));

        Result<SceneView, DomainError> result = service.saveScene(principal(), sessionId, DOC);

        assertThat(result.isErr()).isTrue();
        assertThat(result.unwrapErr()).isEqualTo(notFound);
    }

    @Test
    @DisplayName("save rejects an empty document with a validation error")
    void saveRejectsEmptyDocument() {
        Result<SceneView, DomainError> result = service.saveScene(principal(), sessionId, "  ");

        assertThat(result.isErr()).isTrue();
        assertThat(result.unwrapErr().category()).isEqualTo(DomainError.Category.VALIDATION);
        assertThat(result.unwrapErr().code()).isEqualTo("SCENE_DOCUMENT_REQUIRED");
    }

    @Test
    @DisplayName("get returns the stored Scene when it exists")
    void getReturnsExisting() {
        stubSessionState(ReviewSessionState.COMPLETED);
        Scene existing = new Scene(sessionId.getValue(), DOC, NOW);
        when(sceneRepository.findById(sessionId.getValue())).thenReturn(Optional.of(existing));

        Result<SceneView, DomainError> result = service.getScene(principal(), sessionId);

        assertThat(result.isOk()).isTrue();
        assertThat(result.unwrap().document()).isEqualTo(DOC);
    }

    @Test
    @DisplayName("get returns SCENE_NOT_FOUND when no Scene exists")
    void getReturnsNotFound() {
        stubSessionState(ReviewSessionState.RECORDING);
        when(sceneRepository.findById(sessionId.getValue())).thenReturn(Optional.empty());

        Result<SceneView, DomainError> result = service.getScene(principal(), sessionId);

        assertThat(result.isErr()).isTrue();
        assertThat(result.unwrapErr().category()).isEqualTo(DomainError.Category.NOT_FOUND);
        assertThat(result.unwrapErr().code()).isEqualTo("SCENE_NOT_FOUND");
    }

    @Test
    @DisplayName("get propagates the rs facade error")
    void getPropagatesSessionError() {
        DomainError notFound = DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found");
        when(reviewSessionFacade.getReviewSession(any(), any())).thenReturn(Result.err(notFound));

        Result<SceneView, DomainError> result = service.getScene(principal(), sessionId);

        assertThat(result.isErr()).isTrue();
        assertThat(result.unwrapErr()).isEqualTo(notFound);
    }
}
