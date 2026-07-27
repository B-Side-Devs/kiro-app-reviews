package io.github.bsidedevs.api_review.rest.dto;

import io.github.bsidedevs.api_review.rs.ReviewSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReviewSessionDto {

    private ReviewSessionDto() {
    }

    public record CreateRequest(UUID projectId) {
    }

    public record Response(
            UUID reviewSessionId,
            UUID projectId,
            UUID createdByUserId,
            String state,
            String persistenceStatus,
            Instant createdAt,
            Instant startedRecordingAt,
            Instant completedAt,
            Instant reopenedAt,
            Instant archivedAt,
            Instant deletedAt) {

        public static Response from(ReviewSession session) {
            return new Response(
                    session.getReviewSessionId(),
                    session.getProjectId(),
                    session.getCreatedByUserId(),
                    session.getState().name(),
                    session.getPersistenceStatus().name(),
                    session.getCreatedAt(),
                    session.getStartedRecordingAt(),
                    session.getCompletedAt(),
                    session.getReopenedAt(),
                    session.getArchivedAt(),
                    session.getDeletedAt());
        }
    }

    public record ListResponse(List<Response> sessions, int total) {

        public static ListResponse from(List<ReviewSession> sessions) {
            List<Response> responses = sessions.stream().map(Response::from).toList();
            return new ListResponse(responses, responses.size());
        }
    }

    public record ErrorResponse(String error, String message) {
    }
}
