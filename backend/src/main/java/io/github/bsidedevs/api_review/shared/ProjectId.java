package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for a Project — the logical grouping of Review Sessions
 * associated with a web application or site under review.
 */
public final class ProjectId extends Id<ProjectId> {

    private static final long serialVersionUID = 1L;

    private ProjectId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static ProjectId generate() {
        return new ProjectId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static ProjectId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new ProjectId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static ProjectId parse(String value) {
        return new ProjectId(UuidV7Generator.parse(value));
    }
}
