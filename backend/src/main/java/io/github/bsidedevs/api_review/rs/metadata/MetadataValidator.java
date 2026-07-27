package io.github.bsidedevs.api_review.rs.metadata;

import io.github.bsidedevs.api_review.shared.DomainError;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validator for Review Session metadata fields.
 *
 * <p>Validates:
 * - name: absent or 1-200 chars after trim
 * - description: absent or 1-2000 chars
 * - targetUrl: absent or http(s) URL ≤2048 chars
 *
 * <p>Used in CREATE and UPDATE_METADATA operations to validate
 * {@link NewSessionData} and {@link MetadataPatch} before persistence.
 */
public final class MetadataValidator {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_URL_LENGTH = 2048;

    private MetadataValidator() {
    }

    /**
     * Validates metadata for a new Review Session.
     *
     * @param name optional session name
     * @param description optional description
     * @param targetUrl optional target URL
     * @return empty list if valid, list of validation errors otherwise
     */
    public static List<DomainError.FieldError> validate(
            Optional<String> name,
            Optional<String> description,
            Optional<String> targetUrl
    ) {
        List<DomainError.FieldError> errors = new ArrayList<>();

        validateName(name).ifPresent(errors::add);
        validateDescription(description).ifPresent(errors::add);
        validateTargetUrl(targetUrl).ifPresent(errors::add);

        return errors;
    }

    /**
     * Validates a name field.
     *
     * <p>Rules:
     * - If absent (empty Optional), validation passes
     * - If present, trimmed length must be 1-200 characters
     *
     * @param name optional name value
     * @return FieldError if invalid, empty Optional if valid
     */
    public static Optional<DomainError.FieldError> validateName(Optional<String> name) {
        if (name.isEmpty()) {
            return Optional.empty();
        }

        String trimmed = name.get().trim();
        if (trimmed.isEmpty()) {
            return Optional.of(new DomainError.FieldError("name", "name must not be empty"));
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            return Optional.of(new DomainError.FieldError("name", "name must be at most " + MAX_NAME_LENGTH + " characters"));
        }

        return Optional.empty();
    }

    /**
     * Validates a description field.
     *
     * <p>Rules:
     * - If absent (empty Optional), validation passes
     * - If present, length must be 1-2000 characters
     *
     * @param description optional description value
     * @return FieldError if invalid, empty Optional if valid
     */
    public static Optional<DomainError.FieldError> validateDescription(Optional<String> description) {
        if (description.isEmpty()) {
            return Optional.empty();
        }

        String desc = description.get();
        if (desc.isEmpty()) {
            return Optional.of(new DomainError.FieldError("description", "description must not be empty"));
        }
        if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            return Optional.of(new DomainError.FieldError("description", "description must be at most " + MAX_DESCRIPTION_LENGTH + " characters"));
        }

        return Optional.empty();
    }

    /**
     * Validates a target URL field.
     *
     * <p>Rules:
     * - If absent (empty Optional), validation passes
     * - If present, must be valid http(s) URL with length ≤2048 chars
     *
     * @param targetUrl optional URL value
     * @return FieldError if invalid, empty Optional if valid
     */
    public static Optional<DomainError.FieldError> validateTargetUrl(Optional<String> targetUrl) {
        if (targetUrl.isEmpty()) {
            return Optional.empty();
        }

        String url = targetUrl.get();
        if (url.isEmpty()) {
            return Optional.of(new DomainError.FieldError("targetUrl", "targetUrl must not be empty"));
        }
        if (url.length() > MAX_URL_LENGTH) {
            return Optional.of(new DomainError.FieldError("targetUrl", "targetUrl must be at most " + MAX_URL_LENGTH + " characters"));
        }

        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return Optional.of(new DomainError.FieldError("targetUrl", "targetUrl must use http or https protocol"));
            }
        } catch (MalformedURLException e) {
            return Optional.of(new DomainError.FieldError("targetUrl", "targetUrl must be a valid URL"));
        }

        return Optional.empty();
    }
}
