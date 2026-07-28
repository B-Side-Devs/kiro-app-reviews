package io.github.bsidedevs.api_review.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.bsidedevs.api_review.rs.ReviewSessionFacade.ReviewSessionView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO definitions for the Review Session REST API.
 *
 * <p>Maps between the facade's {@link ReviewSessionView} and the JSON
 * representations consumed/produced by the controller. Never references
 * the JPA entity directly (Requirement 1.6).
 */
public final class ReviewSessionDto {

    private ReviewSessionDto() {
    }

    // ---- Request DTOs ----

    /**
     * Request body for creating a new Review Session.
     *
     * <p>projectId comes from the URL path; only metadata fields are in the body.
     *
     * @param name optional session name
     * @param description optional description
     * @param targetUrl optional target URL
     */
    public record CreateRequest(
            String name,
            String description,
            String targetUrl
    ) {
    }

    // ---- Response DTOs ----

    /**
     * JSON representation of a Review Session (14 fields matching ReviewSessionView).
     *
     * <p>Null-valued optional fields are included in JSON output to provide
     * a consistent schema for clients.
     */
    public record Response(
            UUID reviewSessionId,
            UUID projectId,
            UUID createdByUserId,
            String state,
            String persistenceStatus,
            String name,
            String description,
            String targetUrl,
            Instant createdAt,
            Instant startedRecordingAt,
            Instant completedAt,
            Instant reopenedAt,
            Instant archivedAt,
            Instant deletedAt
    ) {

        /**
         * Maps a {@link ReviewSessionView} to the REST response representation.
         *
         * @param view the facade view record
         * @return response DTO ready for JSON serialization
         */
        public static Response from(ReviewSessionView view) {
            return new Response(
                    view.reviewSessionId().getValue(),
                    view.projectId().getValue(),
                    view.createdByUserId().getValue(),
                    view.state().name(),
                    view.persistenceStatus().name(),
                    view.name().orElse(null),
                    view.description().orElse(null),
                    view.targetUrl().orElse(null),
                    view.createdAt(),
                    view.startedRecordingAt().orElse(null),
                    view.completedAt().orElse(null),
                    view.reopenedAt().orElse(null),
                    view.archivedAt().orElse(null),
                    view.deletedAt().orElse(null)
            );
        }
    }

    // ---- Error DTOs (Problem+JSON, RFC 7807) ----

    /**
     * Problem+JSON error response following RFC 7807.
     *
     * <p>Null-valued extension fields are excluded from the JSON output
     * to keep error responses lean.
     *
     * @param type URI identifying the problem type
     * @param title short human-readable summary
     * @param status HTTP status code
     * @param detail human-readable explanation
     * @param instance correlation ID as URI (request-scoped)
     * @param code domain error code
     * @param currentState current state when transition failed (nullable)
     * @param errors field-level validation errors (nullable)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProblemResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            String currentState,
            List<FieldErrorDto> errors
    ) {
    }

    /**
     * Field-level validation error detail for Problem+JSON responses.
     *
     * @param field the field name that failed validation
     * @param message human-readable error description
     */
    public record FieldErrorDto(
            String field,
            String message
    ) {
    }
}
