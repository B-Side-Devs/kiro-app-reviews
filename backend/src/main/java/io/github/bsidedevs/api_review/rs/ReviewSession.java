package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Review Session aggregate. State machine:
 * DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED → DELETED (terminal).
 */
@Entity
@Table(name = "review_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSession {

    @Id
    private UUID reviewSessionId;

    private UUID projectId;

    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    private ReviewSessionState state;

    @Enumerated(EnumType.STRING)
    private PersistenceStatus persistenceStatus;

    private Instant createdAt;
    private Instant startedRecordingAt;
    private Instant completedAt;
    private Instant reopenedAt;
    private Instant archivedAt;
    private Instant deletedAt;

    public ReviewSession(UUID reviewSessionId, UUID projectId, UUID createdByUserId) {
        this.reviewSessionId = reviewSessionId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.state = ReviewSessionState.DRAFT;
        this.persistenceStatus = PersistenceStatus.OK;
        this.createdAt = Instant.now();
    }

    public boolean startRecording() {
        if (state != ReviewSessionState.DRAFT) {
            return false;
        }
        this.state = ReviewSessionState.RECORDING;
        this.startedRecordingAt = Instant.now();
        return true;
    }

    public boolean completeRecording() {
        if (state != ReviewSessionState.RECORDING || persistenceStatus != PersistenceStatus.OK) {
            return false;
        }
        this.state = ReviewSessionState.COMPLETED;
        this.completedAt = Instant.now();
        return true;
    }

    public boolean reopen() {
        if (state != ReviewSessionState.COMPLETED) {
            return false;
        }
        this.state = ReviewSessionState.REOPENED;
        this.reopenedAt = Instant.now();
        return true;
    }

    public boolean archive() {
        if (state != ReviewSessionState.DRAFT
                && state != ReviewSessionState.COMPLETED
                && state != ReviewSessionState.REOPENED) {
            return false;
        }
        this.state = ReviewSessionState.ARCHIVED;
        this.archivedAt = Instant.now();
        return true;
    }

    public boolean delete() {
        if (state == ReviewSessionState.DELETED) {
            return false;
        }
        this.state = ReviewSessionState.DELETED;
        this.deletedAt = Instant.now();
        return true;
    }

    public void markPersistenceFailed() {
        this.persistenceStatus = PersistenceStatus.FAILED;
    }

    public void markPersistenceInProgress() {
        this.persistenceStatus = PersistenceStatus.IN_PROGRESS;
    }

    public void markPersistenceOk() {
        this.persistenceStatus = PersistenceStatus.OK;
    }
}
