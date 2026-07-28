package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.rs.metadata.FieldPatch;
import io.github.bsidedevs.api_review.rs.metadata.MetadataPatch;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Facade interface for Review Session domain operations.
 *
 * <p>Single public entry point for all Review Session operations. Exposes
 * both read operations (get, list, currentState) and lifecycle operations
 * (create, updateMetadata, startRecording, completeRecording, reopen, archive, delete).
 *
 * <p>All operations receive the {@link AuthenticatedPrincipal} explicitly and
 * return {@link Result<View, DomainError>} instead of throwing exceptions.
 *
 * <p>Implementation: {@link ReviewSessionService}
 */
public interface ReviewSessionFacade {

    // ---- Types exposed in public signatures ----

    /**
     * View record representing a Review Session for external consumption.
     *
     * <p>14 fields: id, project, creator, state, persistence status,
     * 3 metadata fields (optional), 6 timestamps (5 optional).
     *
     * @param reviewSessionId unique identifier
     * @param projectId project identifier
     * @param createdByUserId user identifier
     * @param state current state
     * @param persistenceStatus persistence status
     * @param name optional session name
     * @param description optional description
     * @param targetUrl optional target URL
     * @param createdAt creation timestamp
     * @param startedRecordingAt optional start timestamp
     * @param completedAt optional completion timestamp
     * @param reopenedAt optional reopen timestamp
     * @param archivedAt optional archive timestamp
     * @param deletedAt optional deletion timestamp
     */
    public record ReviewSessionView(
            ReviewSessionId reviewSessionId,
            ProjectId projectId,
            UserId createdByUserId,
            ReviewSessionState state,
            PersistenceStatus persistenceStatus,
            Optional<String> name,
            Optional<String> description,
            Optional<String> targetUrl,
            Instant createdAt,
            Optional<Instant> startedRecordingAt,
            Optional<Instant> completedAt,
            Optional<Instant> reopenedAt,
            Optional<Instant> archivedAt,
            Optional<Instant> deletedAt
    ) {
    }

    /**
     * Data record for creating a new Review Session.
     *
     * <p>All metadata fields are optional. Used in {@link #createSession}.
     *
     * @param name optional session name
     * @param description optional description
     * @param targetUrl optional target URL
     */
    public record NewSessionData(
            Optional<String> name,
            Optional<String> description,
            Optional<String> targetUrl
    ) {
    }

    /**
     * Scope for listing Review Sessions.
     *
     * <p>- {@link #ACTIVE}: excludes ARCHIVED and DELETED
     * - {@link #ALL}: includes ARCHIVED, excludes DELETED
     */
    public enum ListingScope {

        /** Active sessions only: excludes ARCHIVED and DELETED. */
        ACTIVE,

        /** All sessions including archived: excludes DELETED. */
        ALL
    }

    // ---- Read operations (Requirement 1.1) ----

    /**
     * Retrieves a single Review Session by ID.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the session or error
     */
    Result<ReviewSessionView, DomainError> getReviewSession(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    /**
     * Lists Review Sessions for a project.
     *
     * @param principal authenticated principal making the request
     * @param projectId project identifier
     * @param scope listing scope (ACTIVE or ALL)
     * @return list of session views or error
     */
    Result<List<ReviewSessionView>, DomainError> listSessions(
            AuthenticatedPrincipal principal,
            ProjectId projectId,
            ListingScope scope
    );

    /**
     * Gets the current state of a Review Session.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return current state or error
     */
    Result<ReviewSessionState, DomainError> currentState(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    // ---- Lifecycle operations (Requirement 1.2) ----

    /**
     * Creates a new Review Session.
     *
     * @param principal authenticated principal making the request
     * @param projectId project identifier
     * @param data session metadata
     * @return view of the created session or error
     */
    Result<ReviewSessionView, DomainError> createSession(
            AuthenticatedPrincipal principal,
            ProjectId projectId,
            NewSessionData data
    );

    /**
     * Updates metadata of an existing Review Session.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @param patch metadata patch instructions
     * @return view of the updated session or error
     */
    Result<ReviewSessionView, DomainError> updateMetadata(
            AuthenticatedPrincipal principal,
            ReviewSessionId id,
            MetadataPatch patch
    );

    /**
     * Starts recording for a Review Session.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the updated session or error
     */
    Result<ReviewSessionView, DomainError> startRecording(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    /**
     * Completes recording for a Review Session.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the updated session or error
     */
    Result<ReviewSessionView, DomainError> completeRecording(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    /**
     * Reopens a completed Review Session.
     *
     * <p>OWNER reason required in access decision.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the updated session or error
     */
    Result<ReviewSessionView, DomainError> reopen(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    /**
     * Archives a completed Review Session.
     *
     * <p>OWNER reason required in access decision.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the updated session or error
     */
    Result<ReviewSessionView, DomainError> archive(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );

    /**
     * Deletes (soft delete) a Review Session.
     *
     * <p>OWNER reason required in access decision. DELETED sessions are
     * excluded from all listings and return NOT_FOUND for non-owners.
     *
     * @param principal authenticated principal making the request
     * @param id Review Session identifier
     * @return view of the deleted session or error
     */
    Result<ReviewSessionView, DomainError> delete(
            AuthenticatedPrincipal principal,
            ReviewSessionId id
    );
}
