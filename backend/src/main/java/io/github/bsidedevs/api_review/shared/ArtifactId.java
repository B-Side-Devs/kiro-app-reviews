package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for an Artifact — any content unit captured or created
 * inside a Review Session (recording, snapshot, comment, note, AI result, etc.).
 */
public final class ArtifactId extends Id<ArtifactId> {

    private static final long serialVersionUID = 1L;

    private ArtifactId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static ArtifactId generate() {
        return new ArtifactId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static ArtifactId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new ArtifactId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static ArtifactId parse(String value) {
        return new ArtifactId(UuidV7Generator.parse(value));
    }
}
