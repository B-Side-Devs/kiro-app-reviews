package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.observability.CorrelationContext;
import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.ListingScope;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.NewSessionData;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.ReviewSessionView;
import io.github.bsidedevs.api_review.rs.metadata.FieldPatch;
import io.github.bsidedevs.api_review.rs.metadata.MetadataPatch;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.ProjectId;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Review Session operations.
 *
 * <p>Routes are split between two prefixes:
 * <ul>
 *   <li>{@code /api/v1/projects/{projectId}/review-sessions} — create and list</li>
 *   <li>{@code /api/v1/review-sessions/{sessionId}} — get, patch, state transitions, delete</li>
 * </ul>
 *
 * <p>Uses the {@link ReviewSessionFacade} for all domain operations.
 * The {@link AuthenticatedPrincipal} is resolved by PrincipalArgumentResolver
 * (set as request attribute by IdentityFilter).
 */
@RestController
@RequiredArgsConstructor
public class ReviewSessionController {

    private final ReviewSessionFacade reviewSessionFacade;
    private final ProblemFactory problemFactory;

    // ---- Create and List (project-scoped) ----

    @PostMapping("/api/v1/projects/{projectId}/review-sessions")
    public ResponseEntity<?> create(
            @PathVariable String projectId,
            @RequestBody ReviewSessionDto.CreateRequest request,
            AuthenticatedPrincipal principal) {

        ProjectId parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null) {
            return toValidationErrorResponse("projectId", "Invalid project ID format: must be a valid UUID v7");
        }

        var data = new NewSessionData(
                java.util.Optional.ofNullable(request.name()),
                java.util.Optional.ofNullable(request.description()),
                java.util.Optional.ofNullable(request.targetUrl())
        );

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.createSession(principal, parsedProjectId, data);

        if (result.isOk()) {
            ReviewSessionView view = result.unwrap();
            ReviewSessionDto.Response response = ReviewSessionDto.Response.from(view);
            URI location = URI.create("/api/v1/review-sessions/" + view.reviewSessionId().getValue());
            return ResponseEntity.created(location).body(response);
        }
        return toErrorResponse(result.unwrapErr());
    }

    @GetMapping("/api/v1/projects/{projectId}/review-sessions")
    public ResponseEntity<?> list(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "ACTIVE") String scope,
            AuthenticatedPrincipal principal) {

        ProjectId parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null) {
            return toValidationErrorResponse("projectId", "Invalid project ID format: must be a valid UUID v7");
        }

        ListingScope listingScope = parseListingScope(scope);

        Result<List<ReviewSessionView>, DomainError> result =
                reviewSessionFacade.listSessions(principal, parsedProjectId, listingScope);

        if (result.isOk()) {
            List<ReviewSessionDto.Response> responses = result.unwrap().stream()
                    .map(ReviewSessionDto.Response::from)
                    .toList();
            return ResponseEntity.ok(responses);
        }
        return toErrorResponse(result.unwrapErr());
    }

    // ---- Get, Patch, State Transitions, Delete (session-scoped) ----

    @GetMapping("/api/v1/review-sessions/{sessionId}")
    public ResponseEntity<?> get(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.getReviewSession(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @PatchMapping("/api/v1/review-sessions/{sessionId}")
    public ResponseEntity<?> updateMetadata(
            @PathVariable String sessionId,
            @RequestBody ObjectNode body,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        MetadataPatch patch = buildMetadataPatch(body);

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.updateMetadata(principal, parsedSessionId, patch);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @PostMapping("/api/v1/review-sessions/{sessionId}/start")
    public ResponseEntity<?> startRecording(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.startRecording(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @PostMapping("/api/v1/review-sessions/{sessionId}/complete")
    public ResponseEntity<?> completeRecording(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.completeRecording(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @PostMapping("/api/v1/review-sessions/{sessionId}/reopen")
    public ResponseEntity<?> reopen(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.reopen(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @PostMapping("/api/v1/review-sessions/{sessionId}/archive")
    public ResponseEntity<?> archive(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.archive(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()));
        }
        return toErrorResponse(result.unwrapErr());
    }

    @DeleteMapping("/api/v1/review-sessions/{sessionId}")
    public ResponseEntity<?> delete(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<ReviewSessionView, DomainError> result =
                reviewSessionFacade.delete(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.noContent().build();
        }
        return toErrorResponse(result.unwrapErr());
    }

    // ---- Private helpers ----

    /**
     * Parses a project ID string into a type-safe ProjectId.
     *
     * @return the parsed ProjectId, or null if parsing fails
     */
    private ProjectId parseProjectId(String projectId) {
        try {
            return ProjectId.parse(projectId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parses a session ID string into a type-safe ReviewSessionId.
     *
     * @return the parsed ReviewSessionId, or null if parsing fails
     */
    private ReviewSessionId parseSessionId(String sessionId) {
        try {
            return ReviewSessionId.parse(sessionId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parses the scope query parameter. Invalid values default to ACTIVE.
     */
    private ListingScope parseListingScope(String scope) {
        try {
            return ListingScope.valueOf(scope.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ListingScope.ACTIVE;
        }
    }

    /**
     * Builds a MetadataPatch from a Jackson ObjectNode.
     *
     * <p>For each field (name, description, targetUrl):
     * <ul>
     *   <li>Field absent → FieldPatch.Unchanged</li>
     *   <li>Field present with null value → FieldPatch.Clear</li>
     *   <li>Field present with string value → FieldPatch.Set(value)</li>
     * </ul>
     */
    private MetadataPatch buildMetadataPatch(ObjectNode body) {
        return new MetadataPatch(
                parseFieldPatch(body, "name"),
                parseFieldPatch(body, "description"),
                parseFieldPatch(body, "targetUrl")
        );
    }

    private FieldPatch<String> parseFieldPatch(ObjectNode body, String fieldName) {
        if (!body.has(fieldName)) {
            return new FieldPatch.Unchanged<>();
        }
        var node = body.get(fieldName);
        if (node.isNull()) {
            return new FieldPatch.Clear<>();
        }
        return new FieldPatch.Set<>(node.asText());
    }

    /**
     * Maps a DomainError to a Problem+JSON ResponseEntity via {@link ProblemFactory}.
     */
    private ResponseEntity<ReviewSessionDto.ProblemResponse> toErrorResponse(DomainError error) {
        return problemFactory.toResponseEntity(error, CorrelationContext.get());
    }

    /**
     * Returns a 400 validation error for invalid path variable parsing.
     */
    private ResponseEntity<ReviewSessionDto.ProblemResponse> toValidationErrorResponse(String field, String message) {
        DomainError error = DomainError.validation(
                "INVALID_ID_FORMAT",
                message,
                List.of(new DomainError.FieldError(field, message))
        );
        return problemFactory.toResponseEntity(error, CorrelationContext.get());
    }

    private HttpStatus mapCategoryToStatus(DomainError.Category category) {
        return problemFactory.statusFor(category);
    }
}
