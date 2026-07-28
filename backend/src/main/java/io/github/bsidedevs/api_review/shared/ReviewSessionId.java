package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for a Review Session — the aggregate root that groups all
 * artifacts captured during a single review.
 */
public final class ReviewSessionId extends Id<ReviewSessionId> {

    private static final long serialVersionUID = 1L;

    private ReviewSessionId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static ReviewSessionId generate() {
        return new ReviewSessionId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static ReviewSessionId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new ReviewSessionId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static ReviewSessionId parse(String value) {
        return new ReviewSessionId(UuidV7Generator.parse(value));
    }
}
