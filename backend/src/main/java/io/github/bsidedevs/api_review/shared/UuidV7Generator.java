package io.github.bsidedevs.api_review.shared;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Generator for UUID v7 identifiers: time-ordered, globally unique, and
 * K-sortable without decoding.
 *
 * <p>Layout (RFC 9562): 48-bit Unix timestamp (ms) | 4-bit version (0111) |
 * 12-bit rand_a | 2-bit variant (10) | 62-bit rand_b.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9562">RFC 9562</a>
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    // UUID v7 version bits (0111 = 7)
    private static final int VERSION = 7;

    // UUID variant bits (10)
    private static final int VARIANT = 2;

    private UuidV7Generator() {
        // Utility class
    }

    /** Generates a new UUID v7 using the current system time. */
    public static UUID generate() {
        return generate(Instant.now());
    }

    /** Generates a UUID v7 with an explicit timestamp; useful for deterministic tests. */
    public static UUID generate(Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");

        // Unix timestamp in milliseconds (48 bits)
        long timestampMs = instant.toEpochMilli();

        // Generate random bits
        long randomA = RANDOM.nextInt() & 0xFFFFFFFFL; // 32 bits
        long randomB = RANDOM.nextLong();

        // Build the UUID
        // Most significant bits: 48-bit timestamp | 4-bit version | 12-bit rand_a
        long msb = (timestampMs << 16) | ((long) VERSION << 12) | (randomA & 0x0FFFL);

        // Least significant bits: 2-bit variant | 62-bit rand_b
        long lsb = ((long) VARIANT << 62) | (randomB & 0x3FFFFFFFFFFFFFFFL);

        return new UUID(msb, lsb);
    }

    /** Parses and validates a UUID v7 from its string form. */
    public static UUID parse(String uuidString) {
        Objects.requireNonNull(uuidString, "UUID string must not be null");
        UUID uuid = UUID.fromString(uuidString);
        validateV7(uuid);
        return uuid;
    }

    /** Throws {@link IllegalArgumentException} if {@code uuid} is not version 7. */
    public static void validateV7(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID must not be null");
        int version = uuid.version();
        if (version != VERSION) {
            throw new IllegalArgumentException(
                "Expected UUID version 7, got version " + version
            );
        }
    }

    /** Extracts the timestamp embedded in a UUID v7. */
    public static Instant extractTimestamp(UUID uuid) {
        validateV7(uuid);
        long msb = uuid.getMostSignificantBits();
        long timestampMs = msb >>> 16;
        return Instant.ofEpochMilli(timestampMs);
    }
}
