package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.observability.SecurityAuditLogger;
import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.rs.metadata.FieldPatch;
import io.github.bsidedevs.api_review.rs.metadata.MetadataPatch;
import io.github.bsidedevs.api_review.rs.metadata.MetadataValidator;
import io.github.bsidedevs.api_review.wspr.WorkspaceProjectFacade;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Review Session domain operations.
 *
 * <p>Implements the {@link ReviewSessionFacade} interface and provides the common
 * execution algorithm with transaction wrapping, principal resolution, authorization
 * checks, metadata validation, state transition evaluation, and proper error handling.
 *
 * <p>All public operations receive an {@link AuthenticatedPrincipal} explicitly and
 * return {@link Result<ReviewSessionView, DomainError>}.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Transaction wrapping around all operations</li>
 *   <li>Authorization via WorkspaceProjectFacade.canAccessProject</li>
 *   <li>State transition evaluation via TransitionMatrix</li>
 *   <li>Metadata validation for CREATE/UPDATE_METADATA</li>
 *   <li>Optimistic locking for concurrency (InvalidTransition for conflicts)</li>
 *   <li>Fail-closed: access denied returns NOT_FOUND (indistinguishable from not found)</li>
 * </ul>
 *
 * <p>Package-private methods (applyStart, applyComplete, etc.) handle state mutations
 * without validation - the public facade ensures all validations pass before calling.
 *
 * @see ReviewSessionFacade
 * @see TransitionMatrix
 * @see MetadataValidator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSessionService implements ReviewSessionFacade {

    private final ReviewSessionRepository reviewSessionRepository;
    private final WorkspaceProjectFacade workspaceProjectFacade;
    private final SecurityAuditLogger securityAuditLogger;
    private final Clock clock;

    // ---- Read operations ----

    @Override
    @Transactional(readOnly = true)
    public Result<ReviewSessionView, DomainError> getReviewSession(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        var sessionOpt = reviewSessionRepository.findById(id.getValue());

        // Determine projectId: from session if present (required to evaluate access)
        // If session is absent, we cannot evaluate canAccessProject → return NOT_FOUND
        // (indistinguishable from access denied per Requirement 5.7)
        if (sessionOpt.isEmpty()) {
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        var session = sessionOpt.get();
        var projectId = ProjectId.of(session.getProjectId());

        // canAccessProject ALWAYS first, before revealing any resource data (Requirement 5.1)
        var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
        if (!accessDecision.granted()) {
            securityAuditLogger.denied(principal.userId(), id, accessDecision.reason(), principal.correlationId());
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        // Check if session was deleted and principal is not owner (Requirement 3.6)
        if (session.getState() == ReviewSessionState.DELETED
                && accessDecision.reason() != AccessReason.OWNER) {
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        return Result.ok(toView(session));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ReviewSessionView>, DomainError> listSessions(
            AuthenticatedPrincipal principal,
            ProjectId projectId,
            ListingScope scope) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
        if (!accessDecision.granted()) {
            securityAuditLogger.denied(principal.userId(), projectId, accessDecision.reason(), principal.correlationId());
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Project not found or access denied"));
        }

        List<ReviewSession> sessions;
        if (scope == ListingScope.ACTIVE) {
            sessions = reviewSessionRepository.findForListing(
                    projectId.getValue(),
                    List.of(ReviewSessionState.ARCHIVED, ReviewSessionState.DELETED),
                    PageRequest.of(0, 200));
        } else {
            // ALL scope: excludes only DELETED (Listado Histórico includes ARCHIVED)
            sessions = reviewSessionRepository.findForListing(
                    projectId.getValue(),
                    List.of(ReviewSessionState.DELETED),
                    PageRequest.of(0, 200));
        }

        return Result.ok(sessions.stream()
                .map(this::toView)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ReviewSessionState, DomainError> currentState(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        var sessionOpt = reviewSessionRepository.findById(id.getValue());
        if (sessionOpt.isEmpty()) {
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        var session = sessionOpt.get();
        var projectId = ProjectId.of(session.getProjectId());

        // canAccessProject ALWAYS first, before revealing any resource data (Requirement 5.1)
        var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
        if (!accessDecision.granted()) {
            securityAuditLogger.denied(principal.userId(), id, accessDecision.reason(), principal.correlationId());
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        return Result.ok(session.getState());
    }

    // ---- Lifecycle operations ----

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> createSession(
            AuthenticatedPrincipal principal,
            ProjectId projectId,
            NewSessionData data) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        // canAccessProject ALWAYS first (Requirement 5.1): before validation and before creating
        var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
        if (!accessDecision.granted()) {
            securityAuditLogger.denied(principal.userId(), projectId, accessDecision.reason(), principal.correlationId());
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Project not found or access denied"));
        }

        // Validate metadata after authorization
        var validationErrors = MetadataValidator.validate(
                data.name(),
                data.description(),
                data.targetUrl());
        if (!validationErrors.isEmpty()) {
            return Result.err(DomainError.validation(
                    "METADATA_INVALID",
                    "Metadata validation failed",
                    validationErrors));
        }

        // Create new session
        var now = clock.instant();
        var reviewSessionId = ReviewSessionId.generate();
        var session = new ReviewSession(
            reviewSessionId.getValue(),
            projectId.getValue(),
            principal.userId().getValue(),
            now
        );
        
        var saved = reviewSessionRepository.save(session);
        log.info("Created review session {} for project {}", saved.getReviewSessionId(), projectId);
        return Result.ok(toView(saved));
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> updateMetadata(
            AuthenticatedPrincipal principal,
            ReviewSessionId id,
            MetadataPatch patch) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        var sessionOpt = reviewSessionRepository.findById(id.getValue());
        if (sessionOpt.isEmpty()) {
            return Result.err(DomainError.notFound("SESSION_NOT_FOUND", "Review session not found"));
        }

        var session = sessionOpt.get();
        var projectId = ProjectId.of(session.getProjectId());

        // Check authorization FIRST (Requirement 5.1)
        var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
        if (!accessDecision.granted()) {
            securityAuditLogger.denied(principal.userId(), id, accessDecision.reason(), principal.correlationId());
            return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
        }

        // Validate metadata patch
        var validationErrors = validateMetadataPatch(patch);
        if (!validationErrors.isEmpty()) {
            return Result.err(DomainError.validation(
                    "METADATA_INVALID",
                    "Metadata validation failed",
                    validationErrors));
        }

        // Check transition validity (only from DRAFT)
        var verdict = TransitionMatrix.evaluate(session.getState(), TransitionMatrix.Command.UPDATE_METADATA, session.getPersistenceStatus());
        if (!verdict.allowed()) {
            return Result.err(DomainError.invalidTransition(
                    verdict.code().orElse("INVALID_TRANSITION"),
                    "Metadata update not allowed from current state",
                    session.getState()));
        }

        // Apply metadata update
        var now = clock.instant();
        applyMetadata(session, patch, now);

        var saved = reviewSessionRepository.save(session);
        log.info("Updated metadata for review session {}", id);
        return Result.ok(toView(saved));
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> startRecording(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        return executeTransition(principal, id, TransitionMatrix.Command.START, null);
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> completeRecording(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        return executeTransition(principal, id, TransitionMatrix.Command.COMPLETE, null);
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> reopen(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        return executeTransition(principal, id, TransitionMatrix.Command.REOPEN, null);
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> archive(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        return executeTransition(principal, id, TransitionMatrix.Command.ARCHIVE, null);
    }

    @Override
    @Transactional
    public Result<ReviewSessionView, DomainError> delete(
            AuthenticatedPrincipal principal,
            ReviewSessionId id) {

        return executeTransition(principal, id, TransitionMatrix.Command.DELETE, null);
    }

    // ---- Internal execution methods ----

    /**
     * Common execution algorithm for lifecycle operations.
     * Handles authorization, state transition evaluation, and persistence.
     */
    private Result<ReviewSessionView, DomainError> executeTransition(
            AuthenticatedPrincipal principal,
            ReviewSessionId id,
            TransitionMatrix.Command command,
            NewSessionData createData) {

        if (principal == null) {
            return Result.err(DomainError.unauthenticated("PRINCIPAL_REQUIRED", "Principal must not be null"));
        }

        try {
            // Get session from DB
            var sessionOpt = reviewSessionRepository.findById(id.getValue());

            // For non-CREATE operations: if session is absent we still need a projectId to call
            // canAccessProject. Since we don't have one, return NOT_FOUND (same as access denied).
            if (sessionOpt.isEmpty()) {
                return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
            }

            var session = sessionOpt.get();
            var projectId = ProjectId.of(session.getProjectId());

            // canAccessProject ALWAYS first, before revealing existence (Requirement 5.1)
            var accessDecision = workspaceProjectFacade.canAccessProject(principal, projectId);
            if (!accessDecision.granted()) {
                securityAuditLogger.denied(principal.userId(), id, accessDecision.reason(), principal.correlationId());
                return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
            }

            // Check if session was deleted and principal is not owner (Requirement 3.6)
            if (session.getState() == ReviewSessionState.DELETED
                    && accessDecision.reason() != AccessReason.OWNER) {
                return Result.err(DomainError.notFound("RESOURCE_NOT_FOUND", "Review session not found"));
            }

            // OWNER-only operations check (Requirements 5.9, 5.10, 3.9)
            if (command == TransitionMatrix.Command.REOPEN
                    || command == TransitionMatrix.Command.ARCHIVE
                    || command == TransitionMatrix.Command.DELETE) {
                if (accessDecision.reason() != AccessReason.OWNER) {
                    securityAuditLogger.denied(principal.userId(), id, accessDecision.reason(), principal.correlationId());
                    return Result.err(DomainError.notAuthorized(
                            "OWNER_ONLY",
                            "Operation requires project ownership"));
                }
            }

            // Evaluate state transition
            var verdict = TransitionMatrix.evaluate(session.getState(), command, session.getPersistenceStatus());
            if (!verdict.allowed()) {
                return Result.err(DomainError.invalidTransition(
                        verdict.code().orElse("INVALID_TRANSITION"),
                        "State transition not allowed",
                        session.getState()));
            }

            // Apply mutation and save
            var now = clock.instant();
            applyMutation(session, command, now);

            try {
                var saved = reviewSessionRepository.save(session);
                log.info("Applied {} to review session {}", command, id);
                return Result.ok(toView(saved));
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic lock failure for session {}", id, e);
                // Reload to get current state for error response
                var freshOpt = reviewSessionRepository.findById(id.getValue());
                if (freshOpt.isPresent()) {
                    return Result.err(DomainError.invalidTransition(
                            "CONCURRENT_TRANSITION",
                            "Another request modified this session concurrently",
                            freshOpt.get().getState()));
                }
                return Result.err(DomainError.invalidTransition(
                        "SESSION_DELETED",
                        "Session was deleted by another request",
                        null));
            }

        } catch (IllegalArgumentException e) {
            log.error("Validation error for session {} command {}", id, command, e);
            return Result.err(DomainError.validation(
                    "VALIDATION_ERROR",
                    "Invalid operation parameters",
                    List.of(new DomainError.FieldError("request", e.getMessage()))));
        } catch (Exception e) {
            log.error("Unexpected error for session {} command {}", id, command, e);
            return Result.err(DomainError.internal(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred"));
        }
    }

    // ---- Mutation methods (package-private) ----

    /**
     * Applies metadata from NewSessionData to a session.
     */
    void applyMetadata(ReviewSession session, NewSessionData data, Instant now) {
        if (data.name().isPresent()) {
            session.name = data.name().get();
        }
        if (data.description().isPresent()) {
            session.description = data.description().get();
        }
        if (data.targetUrl().isPresent()) {
            session.targetUrl = data.targetUrl().get();
        }
    }

    /**
     * Applies metadata from MetadataPatch to a session.
     */
    void applyMetadata(ReviewSession session, MetadataPatch patch, Instant now) {
        // Apply name patch
        if (patch.name() instanceof FieldPatch.Set<String> set) {
            session.name = set.value();
        } else if (patch.name() instanceof FieldPatch.Clear<?>) {
            session.name = null;
        }
        // UNCHANGED leaves the value as-is

        // Apply description patch
        if (patch.description() instanceof FieldPatch.Set<String> set) {
            session.description = set.value();
        } else if (patch.description() instanceof FieldPatch.Clear<?>) {
            session.description = null;
        }

        // Apply targetUrl patch
        if (patch.targetUrl() instanceof FieldPatch.Set<String> set) {
            session.targetUrl = set.value();
        } else if (patch.targetUrl() instanceof FieldPatch.Clear<?>) {
            session.targetUrl = null;
        }
    }

    /**
     * Applies a state mutation to a session.
     * No validation - caller must ensure transition is valid.
     */
    void applyMutation(ReviewSession session, TransitionMatrix.Command command, Instant now) {
        switch (command) {
            case START -> applyStart(session, now);
            case COMPLETE -> applyComplete(session, now);
            case REOPEN -> applyReopen(session, now);
            case ARCHIVE -> applyArchive(session, now);
            case DELETE -> applyDelete(session, now);
            case UPDATE_METADATA, CREATE -> {
                // Handled separately
            }
        }
    }

    /**
     * Applies START transition (DRAFT/REOPENED → RECORDING).
     * No validation - caller must ensure transition is valid.
     */
    void applyStart(ReviewSession session, Instant now) {
        session.applyStart(now);
    }

    /**
     * Applies COMPLETE transition (RECORDING → COMPLETED).
     * No validation - caller must ensure transition is valid.
     */
    void applyComplete(ReviewSession session, Instant now) {
        session.applyComplete(now);
    }

    /**
     * Applies REOPEN transition (COMPLETED → REOPENED).
     * No validation - caller must ensure transition is valid.
     */
    void applyReopen(ReviewSession session, Instant now) {
        session.applyReopen(now);
    }

    /**
     * Applies ARCHIVE transition (COMPLETED → ARCHIVED).
     * No validation - caller must ensure transition is valid.
     */
    void applyArchive(ReviewSession session, Instant now) {
        session.applyArchive(now);
    }

    /**
     * Applies DELETE transition (any non-DELETED state → DELETED).
     * No validation - caller must ensure transition is valid.
     */
    void applyDelete(ReviewSession session, Instant now) {
        session.applyDelete(now);
    }

    // ---- Conversion ----

    /**
     * Converts a ReviewSession entity to a ReviewSessionView record.
     */
    private ReviewSessionView toView(ReviewSession session) {
        return new ReviewSessionView(
                ReviewSessionId.of(session.reviewSessionId),
                ProjectId.of(session.projectId),
                UserId.of(session.createdByUserId),
                session.state,
                session.persistenceStatus,
                session.name != null && !session.name.isEmpty() ? Optional.of(session.name) : Optional.empty(),
                session.description != null && !session.description.isEmpty() ? Optional.of(session.description) : Optional.empty(),
                session.targetUrl != null && !session.targetUrl.isEmpty() ? Optional.of(session.targetUrl) : Optional.empty(),
                session.createdAt,
                session.startedRecordingAt != null ? Optional.of(session.startedRecordingAt) : Optional.empty(),
                session.completedAt != null ? Optional.of(session.completedAt) : Optional.empty(),
                session.reopenedAt != null ? Optional.of(session.reopenedAt) : Optional.empty(),
                session.archivedAt != null ? Optional.of(session.archivedAt) : Optional.empty(),
                session.deletedAt != null ? Optional.of(session.deletedAt) : Optional.empty()
        );
    }

    /**
     * Validates metadata patch.
     *
     * @param patch metadata patch to validate
     * @return empty list if valid, list of validation errors otherwise
     */
    private List<DomainError.FieldError> validateMetadataPatch(MetadataPatch patch) {
        List<DomainError.FieldError> errors = new ArrayList<>();

        // Validate name field
        if (patch.name() instanceof FieldPatch.Set<String> set) {
            MetadataValidator.validateName(Optional.of(set.value()))
                    .ifPresent(errors::add);
        }

        // Validate description field
        if (patch.description() instanceof FieldPatch.Set<String> set) {
            MetadataValidator.validateDescription(Optional.of(set.value()))
                    .ifPresent(errors::add);
        }

        // Validate targetUrl field
        if (patch.targetUrl() instanceof FieldPatch.Set<String> set) {
            MetadataValidator.validateTargetUrl(Optional.of(set.value()))
                    .ifPresent(errors::add);
        }

        return errors;
    }
}
