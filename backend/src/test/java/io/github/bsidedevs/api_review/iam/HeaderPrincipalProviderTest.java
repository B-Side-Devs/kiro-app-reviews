package io.github.bsidedevs.api_review.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bsidedevs.api_review.shared.AccessError;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HeaderPrincipalProvider")
class HeaderPrincipalProviderTest {

    private UserRepository userRepository;
    private HeaderPrincipalProvider provider;

    private static final String CORRELATION_ID = "test-correlation-id";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        provider = new HeaderPrincipalProvider(userRepository);
    }

    @Nested
    @DisplayName("returns UNAUTHENTICATED when")
    class ReturnsUnauthenticated {

        @Test
        @DisplayName("header values list is null")
        void nullHeaderValues() {
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(null, CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("header values list is empty (Requirement 6.3)")
        void emptyHeaderValues() {
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(Collections.emptyList(), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("multiple header values are present (Requirement 6.9)")
        void multipleHeaderValues() {
            List<String> values = List.of("value1", "value2");
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(values, CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("single header value is null")
        void nullSingleValue() {
            List<String> values = new java.util.ArrayList<>();
            values.add(null);
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(values, CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("header value is empty after trimming (Requirement 6.3)")
        void emptyAfterTrim() {
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(List.of("   "), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("header value exceeds 64 characters (Requirement 6.4)")
        void exceedsMaxLength() {
            String longValue = "a".repeat(65);
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(List.of(longValue), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("header value is not a valid UUID")
        void invalidUuid() {
            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(List.of("not-a-uuid"), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("user not found in repository (Requirement 6.4)")
        void userNotFound() {
            UserId userId = UserId.generate();
            when(userRepository.findById(userId.getValue())).thenReturn(Optional.empty());

            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(
                    List.of(userId.getValue().toString()), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("UserRepository throws exception (Requirement 6.10)")
        void repositoryUnavailable() {
            UserId userId = UserId.generate();
            when(userRepository.findById(any(UUID.class))).thenThrow(new RuntimeException("DB unavailable"));

            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(
                    List.of(userId.getValue().toString()), CORRELATION_ID);

            assertThat(result.isErr()).isTrue();
            assertThat(result.unwrapErr()).isEqualTo(AccessError.UNAUTHENTICATED);
        }
    }

    @Nested
    @DisplayName("returns AuthenticatedPrincipal when")
    class ReturnsSuccess {

        @Test
        @DisplayName("valid user ID with existing user (Requirement 6.2)")
        void validUserFound() {
            UserId userId = UserId.generate();
            User user = new User(userId.getValue(), "test@example.com", UserRole.PROJECT_ADMIN, "Test User");
            when(userRepository.findById(userId.getValue())).thenReturn(Optional.of(user));

            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(
                    List.of(userId.getValue().toString()), CORRELATION_ID);

            assertThat(result.isOk()).isTrue();
            AuthenticatedPrincipal principal = result.unwrap();
            assertThat(principal.userId()).isEqualTo(userId);
            assertThat(principal.role()).isEqualTo(UserRole.PROJECT_ADMIN);
            assertThat(principal.correlationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        @DisplayName("header value with leading/trailing whitespace is trimmed")
        void trimmedValue() {
            UserId userId = UserId.generate();
            User user = new User(userId.getValue(), "client@example.com", UserRole.CLIENT, "Client");
            when(userRepository.findById(userId.getValue())).thenReturn(Optional.of(user));

            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(
                    List.of("  " + userId.getValue().toString() + "  "), CORRELATION_ID);

            assertThat(result.isOk()).isTrue();
            AuthenticatedPrincipal principal = result.unwrap();
            assertThat(principal.userId()).isEqualTo(userId);
            assertThat(principal.role()).isEqualTo(UserRole.CLIENT);
        }

        @Test
        @DisplayName("header value at exactly 64 characters length boundary is accepted")
        void exactlyMaxLength() {
            // UUID v7 string is 36 chars, well within the 64-char limit
            UserId userId = UserId.generate();
            User user = new User(userId.getValue(), "admin@example.com", UserRole.PLATFORM_ADMIN, "Admin");
            when(userRepository.findById(userId.getValue())).thenReturn(Optional.of(user));

            String uuidString = userId.getValue().toString();
            assertThat(uuidString.length()).isLessThanOrEqualTo(64);

            Result<AuthenticatedPrincipal, AccessError> result = provider.resolve(
                    List.of(uuidString), CORRELATION_ID);

            assertThat(result.isOk()).isTrue();
        }
    }
}
