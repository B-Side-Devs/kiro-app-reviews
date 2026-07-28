package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for a Workspace — the top-level container a Project Admin
 * uses to organize its Projects and Review Sessions.
 */
public final class WorkspaceId extends Id<WorkspaceId> {

    private static final long serialVersionUID = 1L;

    private WorkspaceId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static WorkspaceId generate() {
        return new WorkspaceId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static WorkspaceId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new WorkspaceId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static WorkspaceId parse(String value) {
        return new WorkspaceId(UuidV7Generator.parse(value));
    }
}
