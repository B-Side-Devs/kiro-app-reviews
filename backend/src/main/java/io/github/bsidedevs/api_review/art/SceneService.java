package io.github.bsidedevs.api_review.art;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.ReviewSessionView;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import io.github.bsidedevs.api_review.shared.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Excalidraw Scene persistence.
 *
 * <p>Reuses the {@link ReviewSessionFacade} to resolve authorization, existence
 * and current state in a single call (fail-closed: access denied is
 * indistinguishable from not found). Enforces that a Scene may only be created or
 * replaced while the Review Session is {@code RECORDING}. Each save fully
 * replaces the previous document (upsert).
 *
 * @see SceneFacade
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SceneService implements SceneFacade {

    private final ReviewSessionFacade reviewSessionFacade;
    private final SceneRepository sceneRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Result<SceneView, DomainError> saveScene(
            AuthenticatedPrincipal principal,
            ReviewSessionId id,
            String document) {

        if (document == null || document.isBlank()) {
            return Result.err(DomainError.validation(
                    "SCENE_DOCUMENT_REQUIRED",
                    "Scene document must not be empty",
                    List.of(new DomainError.FieldError("document", "must not be empty"))));
        }

        // Delegates authorization + existence + deletion semantics to the rs facade.
        var sessionResult = reviewSessionFacade.getReviewSession(principal, id);
        if (sessionResult.isErr()) {
            return Result.err(sessionResult.unwrapErr());
        }

        ReviewSessionView session = sessionResult.unwrap();
        if (session.state() != ReviewSessionState.RECORDING) {
            return Result.err(DomainError.invalidTransition(
                    "SCENE_NOT_EDITABLE",
                    "Scene can only be saved while the Review Session is RECORDING",
                    session.state()));
        }

        Instant now = clock.instant();
        Scene scene = sceneRepository.findById(id.getValue())
                .map(existing -> {
                    existing.replaceDocument(document, now);
                    return existing;
                })
                .orElseGet(() -> new Scene(id.getValue(), document, now));

        Scene saved = sceneRepository.save(scene);
        log.info("Saved scene for review session {}", id);
        return Result.ok(toView(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SceneView, DomainError> getScene(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        // Delegates authorization + existence + deletion semantics to the rs facade.
        var sessionResult = reviewSessionFacade.getReviewSession(principal, id);
        if (sessionResult.isErr()) {
            return Result.err(sessionResult.unwrapErr());
        }

        return sceneRepository.findById(id.getValue())
                .map(scene -> Result.<SceneView, DomainError>ok(toView(scene)))
                .orElseGet(() -> Result.err(DomainError.notFound(
                        "SCENE_NOT_FOUND", "Scene not found for this review session")));
    }

    private SceneView toView(Scene scene) {
        return new SceneView(scene.getDocument(), scene.getCreatedAt(), scene.getUpdatedAt());
    }
}
