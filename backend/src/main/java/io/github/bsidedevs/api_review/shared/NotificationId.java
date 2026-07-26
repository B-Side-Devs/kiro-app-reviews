package io.github.bsidedevs.api_review.shared;

import java.util.UUID;

/**
 * Opaque identifier for a Notification recorded in a recipient's history.
 */
public final class NotificationId extends Id<NotificationId> {

    private static final long serialVersionUID = 1L;

    private NotificationId(UUID value) {
        super(value);
    }

    /** Generates a new id with a fresh UUID v7. */
    public static NotificationId generate() {
        return new NotificationId(UuidV7Generator.generate());
    }

    /** Wraps an existing UUID v7. Throws {@link IllegalArgumentException} otherwise. */
    public static NotificationId of(UUID uuid) {
        UuidV7Generator.validateV7(uuid);
        return new NotificationId(uuid);
    }

    /** Parses a UUID v7 from its string form. */
    public static NotificationId parse(String value) {
        return new NotificationId(UuidV7Generator.parse(value));
    }
}
