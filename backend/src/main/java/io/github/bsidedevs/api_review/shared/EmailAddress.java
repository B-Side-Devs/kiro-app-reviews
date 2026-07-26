package io.github.bsidedevs.api_review.shared;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value type for a validated email address, normalized to lowercase.
 *
 * <p>Uses a pragmatic RFC 5322 pattern covering real-world addresses without
 * validating every edge case of the full spec.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc5322">RFC 5322</a>
 */
public final class EmailAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final String value;

    private EmailAddress(String value) {
        this.value = value;
    }

    /**
     * Validates and wraps an email address.
     *
     * @throws IllegalArgumentException if the address is not RFC 5322 compliant
     */
    public static EmailAddress of(String email) {
        Objects.requireNonNull(email, "Email address must not be null");

        String trimmed = email.trim();
        String normalized = trimmed.toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Invalid email address format: " + email
            );
        }

        return new EmailAddress(normalized);
    }

    /** Returns the parsed address, or {@code null} if invalid. */
    public static EmailAddress tryParse(String email) {
        try {
            return of(email);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public String value() {
        return value;
    }

    /** Checks RFC 5322 validity without constructing an instance. */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String trimmed = email.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmailAddress that = (EmailAddress) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
