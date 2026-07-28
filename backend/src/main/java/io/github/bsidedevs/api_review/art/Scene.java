package io.github.bsidedevs.api_review.art;

import io.github.bsidedevs.api_review.rs.InstantAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Excalidraw Scene associated with a Review Session (1:1).
 *
 * <p>The Scene is an <em>opaque</em> JSON document persisted verbatim in the
 * official Excalidraw {@code .excalidraw} public format. The backend never
 * interprets its internal structure ({@code elements}, {@code appState},
 * {@code files}); validation of that structure is the client's responsibility.
 *
 * <p>The 1:1 relationship is enforced by using {@code reviewSessionId} as the
 * primary key. Each save fully replaces the previous document (upsert); no
 * history is kept. Uses {@code @Version} for optimistic locking and
 * {@link InstantAttributeConverter} for TIMESTAMPTZ(3) mapping, consistent with
 * {@code ReviewSession}.
 */
@Entity
@Table(name = "scenes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scene {

    @Id
    @Column(name = "review_session_id")
    UUID reviewSessionId;

    /** Opaque Excalidraw document, stored as-is in a JSONB column. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document", nullable = false)
    String document;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = InstantAttributeConverter.class)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = InstantAttributeConverter.class)
    Instant updatedAt;

    @Version
    Long version;

    /**
     * Creates a new Scene for a Review Session with its initial document.
     *
     * @param reviewSessionId owning Review Session identifier
     * @param document        opaque Excalidraw JSON document
     * @param now             current timestamp from the injected clock
     */
    public Scene(UUID reviewSessionId, String document, Instant now) {
        this.reviewSessionId = reviewSessionId;
        this.document = document;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Replaces the stored document entirely (upsert semantics) and refreshes the
     * update timestamp. Package-private: callers go through the facade/service.
     *
     * @param document new opaque Excalidraw JSON document
     * @param now      current timestamp from the injected clock
     */
    void replaceDocument(String document, Instant now) {
        this.document = document;
        this.updatedAt = now;
    }
}
