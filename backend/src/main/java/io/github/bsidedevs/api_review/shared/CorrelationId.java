package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier propagated across a request/operation for end-to-end
 * traceability through logs, metrics and traces.
 */
public final class CorrelationId extends Id<CorrelationId> {

    private static final long serialVersionUID = 1L;

    private CorrelationId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static CorrelationId generate() {
        return new CorrelationId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static CorrelationId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new CorrelationId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static CorrelationId parse(String value) {
        return new CorrelationId(UuidV7Generator.parse(value));
    }
}
