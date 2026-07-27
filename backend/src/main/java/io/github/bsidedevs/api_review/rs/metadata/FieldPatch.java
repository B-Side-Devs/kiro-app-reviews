package io.github.bsidedevs.api_review.rs.metadata;

/**
 * Sealed interface for field patch operations.
 *
 * <p>Used in {@link MetadataPatch} to represent three states:
 * - {@link Unchanged}: keep current value
 * - {@link Clear}: remove value (set to absent)
 * - {@link Set}: replace with new value
 *
 * @param <T> the field value type
 */
public sealed interface FieldPatch<T> {

    /**
     * Keep the current value unchanged.
     */
    public record Unchanged<T>() implements FieldPatch<T> {

        public Unchanged {
        }
    }

    /**
     * Clear/remove the current value.
     */
    public record Clear<T>() implements FieldPatch<T> {

        public Clear {
        }
    }

    /**
     * Set a new value.
     *
     * @param value the new value
     */
    public record Set<T>(T value) implements FieldPatch<T> {

        public Set {
        }
    }
}
