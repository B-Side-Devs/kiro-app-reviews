package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Review Session aggregate. State machine:
 * DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED → DELETED (terminal).
 *
 * <p>State mutations are package-private {@code applyXxx(Instant now)} methods
 * that contain NO validation logic. Transition validation lives exclusively in
 * {@link TransitionMatrix}, invoked by {@link ReviewSessionService} before calling
 * these mutators.
 *
 * <p>Uses {@code @Version} for optimistic locking and {@link InstantAttributeConverter}
 * for TIMESTAMPTZ(3) column mapping.
 */
@Entity
@Table(name = "review_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSession {

    @Id
    UUID reviewSessionId;

    UUID projectId;

    UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    ReviewSessionState state;

    @Enumerated(EnumType.STRING)
    PersistenceStatus persistenceStatus;

    String name;
    String description;
    String targetUrl;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = InstantAttributeConverter.class)
    Instant createdAt;

    @Column(name = "started_recording_at")
    @Convert(converter = InstantAttributeConverter.class)
    Instant startedRecordingAt;

    @Column(name = "completed_at")
    @Convert(converter = InstantAttributeConverter.class)
    Instant completedAt;

    @Column(name = "reopened_at")
    @Convert(converter = InstantAttributeConverter.class)
    Instant reopenedAt;

    @Column(name = "archived_at")
    @Convert(converter = InstantAttributeConverter.class)
    Instant archivedAt;

    @Column(name = "deleted_at")
    @Convert(converter = InstantAttributeConverter.class)
    Instant deletedAt;

    @Version
    Long version;

    /**
     * Creates a new ReviewSession in DRAFT state.
     *
     * @param reviewSessionId unique identifier for the session
     * @param projectId       project this session belongs to
     * @param createdByUserId user who created the session
     * @param now             current timestamp from the injected clock
     */
    public ReviewSession(UUID reviewSessionId, UUID projectId, UUID createdByUserId, Instant now) {
        this.reviewSessionId = reviewSessionId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.state = ReviewSessionState.DRAFT;
        this.persistenceStatus = PersistenceStatus.OK;
        this.createdAt = now;
    }

    // ---- Package-private state mutators (no validation) ----

    /**
     * Applies START transition: sets state to RECORDING with timestamp.
     * Caller must ensure transition is valid via TransitionMatrix.
     */
    void applyStart(Instant now) {
        this.state = ReviewSessionState.RECORDING;
        this.startedRecordingAt = now;
    }

    /**
     * Applies COMPLETE transition: sets state to COMPLETED with timestamp.
     * Caller must ensure transition is valid via TransitionMatrix.
     */
    void applyComplete(Instant now) {
        this.state = ReviewSessionState.COMPLETED;
        this.completedAt = now;
    }

    /**
     * Applies REOPEN transition: sets state to REOPENED with timestamp.
     * Caller must ensure transition is valid via TransitionMatrix.
     */
    void applyReopen(Instant now) {
        this.state = ReviewSessionState.REOPENED;
        this.reopenedAt = now;
    }

    /**
     * Applies ARCHIVE transition: sets state to ARCHIVED with timestamp.
     * Caller must ensure transition is valid via TransitionMatrix.
     */
    void applyArchive(Instant now) {
        this.state = ReviewSessionState.ARCHIVED;
        this.archivedAt = now;
    }

    /**
     * Applies DELETE transition: sets state to DELETED with timestamp.
     * Caller must ensure transition is valid via TransitionMatrix.
     */
    void applyDelete(Instant now) {
        this.state = ReviewSessionState.DELETED;
        this.deletedAt = now;
    }

    // ---- Package-private persistence status mutators ----

    void markPersistenceFailed() {
        this.persistenceStatus = PersistenceStatus.FAILED;
    }

    void markPersistenceInProgress() {
        this.persistenceStatus = PersistenceStatus.IN_PROGRESS;
    }

    void markPersistenceOk() {
        this.persistenceStatus = PersistenceStatus.OK;
    }

    // ---- Package-private metadata setters ----

    void setName(String name) {
        this.name = name;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    void setStartedRecordingAt(Instant startedRecordingAt) {
        this.startedRecordingAt = startedRecordingAt;
    }

    void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    void setReopenedAt(Instant reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
