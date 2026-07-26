package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for an authentication Session tracking a logged-in user.
 */
public final class SessionId extends Id<SessionId> {

    private static final long serialVersionUID = 1L;

    private SessionId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static SessionId generate() {
        return new SessionId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static SessionId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new SessionId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static SessionId parse(String value) {
        return new SessionId(UuidV7Generator.parse(value));
    }
}
