package io.github.bsidedevs.api_review.art;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.Result;
import java.time.Instant;

/**
 * Facade for persisting and retrieving the Excalidraw Scene of a Review Session.
 *
 * <p>Single public entry point for Scene operations. All operations receive the
 * {@link AuthenticatedPrincipal} explicitly and return
 * {@link Result}{@code <SceneView, DomainError>} instead of throwing.
 *
 * <p>Authorization and Review Session existence/state are resolved through the
 * {@code rs} facade; this module never re-implements access control.
 *
 * <p>Role in the facade DAG: depends on {@code rs} and {@code iam}; cross-cutting
 * on {@code shared}.
 *
 * <p>Implementation: {@link SceneService}.
 */
public interface SceneFacade {

    /**
     * Opaque view of a persisted Scene for external consumption.
     *
     * @param document  opaque Excalidraw JSON document, returned verbatim
     * @param createdAt when the Scene was first persisted
     * @param updatedAt when the Scene was last replaced
     */
    record SceneView(
            String document,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * Persists the Scene of a Review Session (upsert: fully replaces any previous
     * document).
     *
     * <p>Allowed only while the Review Session is in {@code RECORDING}; otherwise
     * returns an {@code INVALID_TRANSITION} error. Access control, not-found and
     * deletion semantics are delegated to the {@code rs} facade.
     *
     * @param principal authenticated principal making the request
     * @param id        Review Session identifier
     * @param document  opaque Excalidraw JSON document (already validated as JSON)
     * @return the stored Scene view or a domain error
     */
    Result<SceneView, DomainError> saveScene(
            AuthenticatedPrincipal principal,
            ReviewSessionId id,
            String document
    );

    /**
     * Retrieves the last persisted Scene of a Review Session.
     *
     * @param principal authenticated principal making the request
     * @param id        Review Session identifier
     * @return the stored Scene view or a domain error (NOT_FOUND if none exists)
     */
    Result<SceneView, DomainError> getScene(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );
}
