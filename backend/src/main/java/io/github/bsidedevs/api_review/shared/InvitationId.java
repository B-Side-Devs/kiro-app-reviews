package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for an Invitation — the mechanism a Project Admin uses to
 * grant a Client access to a specific Project.
 */
public final class InvitationId extends Id<InvitationId> {

    private static final long serialVersionUID = 1L;

    private InvitationId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static InvitationId generate() {
        return new InvitationId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static InvitationId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new InvitationId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static InvitationId parse(String value) {
        return new InvitationId(UuidV7Generator.parse(value));
    }
}
