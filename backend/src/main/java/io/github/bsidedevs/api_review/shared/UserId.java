package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for a User (Project Admin, Client, or Platform Admin).
 */
public final class UserId extends Id<UserId> {

    private static final long serialVersionUID = 1L;

    private UserId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static UserId generate() {
        return new UserId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static UserId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new UserId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static UserId parse(String value) {
        return new UserId(UuidV7Generator.parse(value));
    }
}
