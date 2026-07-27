package io.github.bsidedevs.api_review.shared;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for opaque identifiers backed by UUID v7. Identifiers are
 * value-typed wrappers exposing only equality/ordering semantics; internal
 * structure is never revealed. UUID v7 keeps identifiers time-ordered and unique.
 *
 * @param <T> the concrete identifier type for self-referencing
 */
public abstract sealed class Id<T extends Id<T>> implements Serializable, Comparable<T>
    permits UserId, WorkspaceId, ProjectId, ReviewSessionId, ArtifactId,
            InvitationId, NotificationId, SessionId, CorrelationId {

    private static final long serialVersionUID = 1L;

    protected final UUID value;

    protected Id(UUID value) {
        this.value = Objects.requireNonNull(value, "Identifier value must not be null");
    }

    // Package-private on purpose: identifiers stay opaque outside `shared`.
    UUID value() {
        return value;
    }

    /**
     * Gets the underlying UUID value.
     * Public for use by domain service classes that need to interact with persistence layers.
     *
     * @return the underlying UUID value
     */
    public UUID getValue() {
        return value;
    }

    @Override
    public int compareTo(T other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Id<?> that = (Id<?>) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
