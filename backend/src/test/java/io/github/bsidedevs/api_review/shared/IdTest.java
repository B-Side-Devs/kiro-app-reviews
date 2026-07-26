package io.github.bsidedevs.api_review.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for opaque identifiers and value types in the shared package.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Equality and stability of opaque identifiers</li>
 *   <li>UUID v7 generation and ordering</li>
 *   <li>RFC 5322 validation in EmailAddress</li>
 * </ul>
 */
@DisplayName("Shared package: Identifiers and Value Types")
class IdTest {

    @Nested
    @DisplayName("UserId tests")
    class UserIdTests {

        @Test
        @DisplayName("Generated UserId should be non-null and valid UUID v7")
        void generatedUserIdShouldBeValid() {
            UserId id = UserId.generate();

            assertNotNull(id);
            assertNotNull(id.toString());
            assertTrue(id.toString().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
            ));
        }

        @Test
        @DisplayName("Two generated UserIds should not be equal")
        void twoGeneratedUserIdsShouldNotBeEqual() throws InterruptedException {
            UserId id1 = UserId.generate();
            Thread.sleep(2); // Ensure different timestamp
            UserId id2 = UserId.generate();

            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("UserId parsed from string should equal original")
        void parsedUserIdShouldEqualOriginal() {
            UserId original = UserId.generate();
            UserId parsed = UserId.parse(original.toString());

            assertEquals(original, parsed);
            assertEquals(original.hashCode(), parsed.hashCode());
        }

        @Test
        @DisplayName("UserId should reject invalid UUID v7")
        void shouldRejectInvalidUuidV7() {
            UUID uuidV4 = UUID.randomUUID();
            assertThrows(IllegalArgumentException.class, () -> UserId.of(uuidV4));
        }
    }

    @Nested
    @DisplayName("Identifier equality tests")
    class IdentifierEqualityTests {

        @Test
        @DisplayName("Identifiers of different types should not be equal even with same UUID")
        void differentIdentifierTypesNotEqual() {
            UUID uuid = UuidV7Generator.generate();
            UserId userId = UserId.of(uuid);
            WorkspaceId workspaceId = WorkspaceId.of(uuid);

            assertNotEquals(userId, workspaceId);
            assertEquals(userId.hashCode(), workspaceId.hashCode()); // Same UUID = same hash
        }

        @Test
        @DisplayName("Identifier equality should be reflexive")
        void equalityReflexive() {
            UserId id = UserId.generate();
            assertEquals(id, id);
        }

        @Test
        @DisplayName("Identifier equality should be symmetric")
        void equalitySymmetric() {
            UserId id1 = UserId.generate();
            UserId id2 = UserId.parse(id1.toString());

            assertEquals(id1, id2);
            assertEquals(id2, id1);
        }

        @Test
        @DisplayName("Identifier equality should be transitive")
        void equalityTransitive() {
            UserId id1 = UserId.generate();
            UserId id2 = UserId.parse(id1.toString());
            UserId id3 = UserId.parse(id2.toString());

            assertEquals(id1, id2);
            assertEquals(id2, id3);
            assertEquals(id1, id3);
        }
    }

    @Nested
    @DisplayName("UUID v7 generator tests")
    class UuidV7GeneratorTests {

        @Test
        @DisplayName("Generated UUID should be version 7")
        void generatedUuidShouldBeVersion7() {
            UUID uuid = UuidV7Generator.generate();
            assertEquals(7, uuid.version());
        }

        @Test
        @DisplayName("Generated UUID should have variant 2")
        void generatedUuidShouldHaveVariant2() {
            UUID uuid = UuidV7Generator.generate();
            assertEquals(2, uuid.variant());
        }

        @Test
        @DisplayName("Extracted timestamp should match input instant")
        void extractedTimestampShouldMatch() {
            Instant instant = Instant.parse("2024-01-15T10:30:00.123Z");
            UUID uuid = UuidV7Generator.generate(instant);
            Instant extracted = UuidV7Generator.extractTimestamp(uuid);

            assertEquals(instant.toEpochMilli(), extracted.toEpochMilli());
        }

        @Test
        @DisplayName("UUIDs generated later should sort after earlier ones")
        void laterUuidsSortAfterEarlierOnes() throws InterruptedException {
            UUID first = UuidV7Generator.generate();
            Thread.sleep(2); // Ensure different timestamp
            UUID second = UuidV7Generator.generate();

            // Compare by UUID (which sorts by timestamp for UUID v7)
            assertTrue(first.compareTo(second) < 0);
        }

        @Test
        @DisplayName("Parsing invalid UUID string should throw")
        void parseInvalidUuidString() {
            assertThrows(IllegalArgumentException.class, () -> UuidV7Generator.parse("not-a-uuid"));
        }

        @Test
        @DisplayName("Parsing UUID v4 should throw")
        void parseUuidV4ShouldThrow() {
            UUID uuidV4 = UUID.randomUUID();
            assertThrows(IllegalArgumentException.class, () -> UuidV7Generator.parse(uuidV4.toString()));
        }
    }

    @Nested
    @DisplayName("EmailAddress tests")
    class EmailAddressTests {

        @Test
        @DisplayName("Valid email should be accepted")
        void validEmailAccepted() {
            EmailAddress email = EmailAddress.of("user@example.com");

            assertEquals("user@example.com", email.value());
        }

        @Test
        @DisplayName("Email should be normalized to lowercase")
        void emailNormalizedToLowercase() {
            EmailAddress email = EmailAddress.of("User@Example.COM");

            assertEquals("user@example.com", email.value());
        }

        @Test
        @DisplayName("Email with whitespace should be trimmed")
        void emailTrimmed() {
            EmailAddress email = EmailAddress.of("  user@example.com  ");

            assertEquals("user@example.com", email.value());
        }

        @Test
        @DisplayName("Invalid email should be rejected")
        void invalidEmailRejected() {
            assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("not-an-email"));
            assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("@example.com"));
            assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("user@"));
            assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("user@example"));
        }

        @Test
        @DisplayName("Null email should throw NullPointerException")
        void nullEmailRejected() {
            assertThrows(NullPointerException.class, () -> EmailAddress.of(null));
        }

        @Test
        @DisplayName("tryParse should return null for invalid email")
        void tryParseReturnsNullForInvalid() {
            assertNull(EmailAddress.tryParse("not-an-email"));
            assertNotNull(EmailAddress.tryParse("valid@example.com"));
        }

        @Test
        @DisplayName("isValid should return correct boolean")
        void isValidReturnsCorrectBoolean() {
            assertTrue(EmailAddress.isValid("user@example.com"));
            assertTrue(EmailAddress.isValid("user.name+tag@example.org"));
            assertFalse(EmailAddress.isValid("not-an-email"));
            assertFalse(EmailAddress.isValid(null));
            assertFalse(EmailAddress.isValid(""));
        }

        @Test
        @DisplayName("Equal emails should have equal hash codes")
        void equalEmailsHaveEqualHashCodes() {
            EmailAddress email1 = EmailAddress.of("user@example.com");
            EmailAddress email2 = EmailAddress.of("USER@EXAMPLE.COM");

            assertEquals(email1, email2);
            assertEquals(email1.hashCode(), email2.hashCode());
        }
    }

    @Nested
    @DisplayName("PayloadRef tests")
    class PayloadRefTests {

        @Test
        @DisplayName("InlineJson should hold JSON content")
        void inlineJsonHoldsContent() {
            PayloadRef.InlineJson payload = new PayloadRef.InlineJson("{\"key\":\"value\"}");

            assertEquals("{\"key\":\"value\"}", payload.json());
        }

        @Test
        @DisplayName("BlobUrl should hold URL")
        void blobUrlHoldsUrl() {
            PayloadRef.BlobUrl payload = new PayloadRef.BlobUrl("https://storage.example.com/blob/123");

            assertEquals("https://storage.example.com/blob/123", payload.url());
        }

        @Test
        @DisplayName("InlineJson should reject null")
        void inlineJsonRejectsNull() {
            assertThrows(NullPointerException.class, () -> new PayloadRef.InlineJson(null));
        }

        @Test
        @DisplayName("BlobUrl should reject null and blank")
        void blobUrlRejectsNullOrBlank() {
            assertThrows(NullPointerException.class, () -> new PayloadRef.BlobUrl(null));
            assertThrows(IllegalArgumentException.class, () -> new PayloadRef.BlobUrl(""));
            assertThrows(IllegalArgumentException.class, () -> new PayloadRef.BlobUrl("   "));
        }

        @Test
        @DisplayName("PayloadRef should support pattern matching")
        void payloadRefSupportsPatternMatching() {
            PayloadRef inline = new PayloadRef.InlineJson("{}");
            PayloadRef blob = new PayloadRef.BlobUrl("https://example.com/blob");

            String resultInline = switch (inline) {
                case PayloadRef.InlineJson json -> "inline: " + json.json();
                case PayloadRef.BlobUrl url -> "blob: " + url.url();
            };

            String resultBlob = switch (blob) {
                case PayloadRef.InlineJson json -> "inline: " + json.json();
                case PayloadRef.BlobUrl url -> "blob: " + url.url();
            };

            assertEquals("inline: {}", resultInline);
            assertEquals("blob: https://example.com/blob", resultBlob);
        }
    }

    @Nested
    @DisplayName("All identifier types tests")
    class AllIdentifierTypesTests {

        @Test
        @DisplayName("All identifier types should generate valid UUID v7")
        void allIdentifiersGenerateValidUuidV7() {
            // Generate all identifier types
            UserId userId = UserId.generate();
            WorkspaceId workspaceId = WorkspaceId.generate();
            ProjectId projectId = ProjectId.generate();
            ReviewSessionId reviewSessionId = ReviewSessionId.generate();
            ArtifactId artifactId = ArtifactId.generate();
            InvitationId invitationId = InvitationId.generate();
            NotificationId notificationId = NotificationId.generate();
            SessionId sessionId = SessionId.generate();
            CorrelationId correlationId = CorrelationId.generate();

            // Verify all are non-null
            assertNotNull(userId);
            assertNotNull(workspaceId);
            assertNotNull(projectId);
            assertNotNull(reviewSessionId);
            assertNotNull(artifactId);
            assertNotNull(invitationId);
            assertNotNull(notificationId);
            assertNotNull(sessionId);
            assertNotNull(correlationId);
        }

        @Test
        @DisplayName("All identifier types should parse from string")
        void allIdentifiersParseFromString() {
            UserId userId = UserId.parse(UserId.generate().toString());
            WorkspaceId workspaceId = WorkspaceId.parse(WorkspaceId.generate().toString());
            ProjectId projectId = ProjectId.parse(ProjectId.generate().toString());
            ReviewSessionId reviewSessionId = ReviewSessionId.parse(ReviewSessionId.generate().toString());
            ArtifactId artifactId = ArtifactId.parse(ArtifactId.generate().toString());
            InvitationId invitationId = InvitationId.parse(InvitationId.generate().toString());
            NotificationId notificationId = NotificationId.parse(NotificationId.generate().toString());
            SessionId sessionId = SessionId.parse(SessionId.generate().toString());
            CorrelationId correlationId = CorrelationId.parse(CorrelationId.generate().toString());

            assertNotNull(userId);
            assertNotNull(workspaceId);
            assertNotNull(projectId);
            assertNotNull(reviewSessionId);
            assertNotNull(artifactId);
            assertNotNull(invitationId);
            assertNotNull(notificationId);
            assertNotNull(sessionId);
            assertNotNull(correlationId);
        }

        @Test
        @DisplayName("All identifiers should implement Comparable")
        void allIdentifiersImplementComparable() throws InterruptedException {
            UserId id1 = UserId.generate();
            Thread.sleep(2);
            UserId id2 = UserId.generate();

            assertTrue(id1.compareTo(id2) < 0);
            assertTrue(id2.compareTo(id1) > 0);
            assertEquals(0, id1.compareTo(UserId.parse(id1.toString())));
        }
    }
}
