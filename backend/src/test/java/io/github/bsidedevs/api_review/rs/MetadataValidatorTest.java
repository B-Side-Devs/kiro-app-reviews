package io.github.bsidedevs.api_review.rs;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bsidedevs.api_review.rs.metadata.MetadataValidator;
import io.github.bsidedevs.api_review.shared.DomainError;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MetadataValidator}.
 *
 * <p>Covers:
 * <ul>
 *   <li>name: absent or 1-200 chars after trim (Requirement 9.1, 9.2)</li>
 *   <li>description: absent or 1-2000 chars (Requirement 9.4)</li>
 *   <li>targetUrl: absent or valid http(s) URL ≤2048 chars (Requirement 9.5, 9.6)</li>
 *   <li>Property-based tests validating length boundaries (Requirement 13.6)</li>
 * </ul>
 */
class MetadataValidatorTest {

    // ---- name ----------------------------------------------------------------

    @Nested
    @DisplayName("name validation")
    class NameValidation {

        @Test
        @DisplayName("absent name passes validation")
        void absentNamePasses() {
            var errors = MetadataValidator.validate(Optional.empty(), Optional.empty(), Optional.empty());
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("name of 1 char passes")
        void singleCharNamePasses() {
            var result = MetadataValidator.validateName(Optional.of("a"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("name of exactly 200 chars passes")
        void maxLengthNamePasses() {
            String name200 = "a".repeat(200);
            var result = MetadataValidator.validateName(Optional.of(name200));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("name of 201 chars fails")
        void nameTooLongFails() {
            String name201 = "a".repeat(201);
            var result = MetadataValidator.validateName(Optional.of(name201));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("name");
        }

        @Test
        @DisplayName("blank name (whitespace only) fails")
        void blankNameFails() {
            var result = MetadataValidator.validateName(Optional.of("   "));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("name");
        }

        @Test
        @DisplayName("empty string name fails")
        void emptyStringNameFails() {
            var result = MetadataValidator.validateName(Optional.of(""));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("name");
        }

        @Test
        @DisplayName("name with leading/trailing spaces: trim applied before length check")
        void nameTrimedBeforeLengthCheck() {
            // 200 spaces + 1 real char + 200 spaces = 401 total but trim → 1 char → valid
            String paddedName = " ".repeat(200) + "x" + " ".repeat(200);
            var result = MetadataValidator.validateName(Optional.of(paddedName));
            assertThat(result).isEmpty();
        }
    }

    // ---- description ---------------------------------------------------------

    @Nested
    @DisplayName("description validation")
    class DescriptionValidation {

        @Test
        @DisplayName("absent description passes validation")
        void absentDescriptionPasses() {
            var result = MetadataValidator.validateDescription(Optional.empty());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("description of 1 char passes")
        void singleCharDescriptionPasses() {
            var result = MetadataValidator.validateDescription(Optional.of("x"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("description of exactly 2000 chars passes")
        void maxLengthDescriptionPasses() {
            String desc2000 = "d".repeat(2000);
            var result = MetadataValidator.validateDescription(Optional.of(desc2000));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("description of 2001 chars fails")
        void descriptionTooLongFails() {
            String desc2001 = "d".repeat(2001);
            var result = MetadataValidator.validateDescription(Optional.of(desc2001));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("description");
        }

        @Test
        @DisplayName("empty string description fails")
        void emptyStringDescriptionFails() {
            var result = MetadataValidator.validateDescription(Optional.of(""));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("description");
        }
    }

    // ---- targetUrl -----------------------------------------------------------

    @Nested
    @DisplayName("targetUrl validation")
    class TargetUrlValidation {

        @Test
        @DisplayName("absent targetUrl passes validation")
        void absentUrlPasses() {
            var result = MetadataValidator.validateTargetUrl(Optional.empty());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("valid http URL passes")
        void validHttpUrlPasses() {
            var result = MetadataValidator.validateTargetUrl(Optional.of("http://example.com"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("valid https URL passes")
        void validHttpsUrlPasses() {
            var result = MetadataValidator.validateTargetUrl(Optional.of("https://example.com/path?q=1"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ftp URL fails")
        void ftpUrlFails() {
            var result = MetadataValidator.validateTargetUrl(Optional.of("ftp://example.com/file"));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("targetUrl");
        }

        @Test
        @DisplayName("non-URL string fails")
        void plainStringFails() {
            var result = MetadataValidator.validateTargetUrl(Optional.of("not-a-url"));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("targetUrl");
        }

        @Test
        @DisplayName("URL of exactly 2048 chars passes")
        void maxLengthUrlPasses() {
            // Build a valid https URL of exactly 2048 chars
            String base = "https://example.com/";
            String path = "a".repeat(2048 - base.length());
            String url2048 = base + path;
            assertThat(url2048).hasSize(2048);

            var result = MetadataValidator.validateTargetUrl(Optional.of(url2048));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("URL of 2049 chars fails")
        void urlTooLongFails() {
            String base = "https://example.com/";
            String path = "a".repeat(2049 - base.length());
            String url2049 = base + path;
            assertThat(url2049).hasSize(2049);

            var result = MetadataValidator.validateTargetUrl(Optional.of(url2049));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("targetUrl");
        }

        @Test
        @DisplayName("empty string targetUrl fails")
        void emptyStringUrlFails() {
            var result = MetadataValidator.validateTargetUrl(Optional.of(""));
            assertThat(result).isPresent();
            assertThat(result.get().field()).isEqualTo("targetUrl");
        }
    }

    // ---- combined validate() -------------------------------------------------

    @Nested
    @DisplayName("combined validate()")
    class CombinedValidation {

        @Test
        @DisplayName("all absent fields returns empty error list")
        void allAbsentReturnsEmpty() {
            List<DomainError.FieldError> errors = MetadataValidator.validate(
                    Optional.empty(), Optional.empty(), Optional.empty());
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("multiple invalid fields returns all errors")
        void multipleInvalidFieldsReturnsAllErrors() {
            List<DomainError.FieldError> errors = MetadataValidator.validate(
                    Optional.of(""),          // blank name → error
                    Optional.of(""),          // empty description → error
                    Optional.of("not-a-url")  // bad URL → error
            );
            assertThat(errors).hasSize(3);
            assertThat(errors).extracting(DomainError.FieldError::field)
                    .containsExactlyInAnyOrder("name", "description", "targetUrl");
        }

        @Test
        @DisplayName("valid values for all fields returns empty error list")
        void allValidReturnsEmpty() {
            List<DomainError.FieldError> errors = MetadataValidator.validate(
                    Optional.of("My Session"),
                    Optional.of("A description"),
                    Optional.of("https://example.com")
            );
            assertThat(errors).isEmpty();
        }
    }

    // ---- property-based tests ------------------------------------------------
    // Note: jqwik @Property methods must be at the top level of the test class,
    // not inside @Nested classes.

    @Property
    @DisplayName("Property: name of length 1..200 (after trim) always passes")
    void nameLengthInRangeAlwaysPasses(@ForAll("validName") String name) {
        var result = MetadataValidator.validateName(Optional.of(name));
        assertThat(result).isEmpty();
    }

    @Property
    @DisplayName("Property: name over 200 chars always fails")
    void nameTooLongAlwaysFails(@ForAll("longName") String name) {
        var result = MetadataValidator.validateName(Optional.of(name));
        assertThat(result).isPresent();
        assertThat(result.get().field()).isEqualTo("name");
    }

    @Property
    @DisplayName("Property: description of length 1..2000 always passes")
    void descriptionLengthInRangeAlwaysPasses(@ForAll("validDescription") String description) {
        var result = MetadataValidator.validateDescription(Optional.of(description));
        assertThat(result).isEmpty();
    }

    @Property
    @DisplayName("Property: description over 2000 chars always fails")
    void descriptionTooLongAlwaysFails(@ForAll("longDescription") String description) {
        var result = MetadataValidator.validateDescription(Optional.of(description));
        assertThat(result).isPresent();
        assertThat(result.get().field()).isEqualTo("description");
    }

    @Property
    @DisplayName("Property: valid http(s) URL always passes targetUrl validation")
    void validUrlAlwaysPasses(@ForAll("validUrl") String url) {
        var result = MetadataValidator.validateTargetUrl(Optional.of(url));
        assertThat(result).isEmpty();
    }

    // ---- Arbitrary providers ----

    @Provide
    Arbitrary<String> validName() {
        // Generate strings of 1–200 non-whitespace-only chars
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(200)
                .filter(s -> !s.trim().isEmpty());
    }

    @Provide
    Arbitrary<String> longName() {
        // Generate strings strictly longer than 200 chars
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(201)
                .ofMaxLength(300);
    }

    @Provide
    Arbitrary<String> validDescription() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(2000);
    }

    @Provide
    Arbitrary<String> longDescription() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2001)
                .ofMaxLength(2100);
    }

    @Provide
    Arbitrary<String> validUrl() {
        // Generate valid http/https URLs within the 2048-char limit
        return Arbitraries.of("http", "https").flatMap(scheme ->
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(20)
                        .map(host -> scheme + "://" + host + ".com")
        );
    }
}
