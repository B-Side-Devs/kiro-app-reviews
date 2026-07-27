package io.github.bsidedevs.api_review.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

@DisplayName("PrincipalArgumentResolver")
class PrincipalArgumentResolverTest {

    private PrincipalArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PrincipalArgumentResolver();
    }

    @Nested
    @DisplayName("supportsParameter")
    class SupportsParameter {

        @Test
        @DisplayName("returns true for AuthenticatedPrincipal type")
        void supportsPrincipalType() throws Exception {
            MethodParameter param = new MethodParameter(
                    SampleController.class.getMethod("handle", AuthenticatedPrincipal.class), 0);

            assertThat(resolver.supportsParameter(param)).isTrue();
        }

        @Test
        @DisplayName("returns false for String type")
        void doesNotSupportStringType() throws Exception {
            MethodParameter param = new MethodParameter(
                    SampleController.class.getMethod("handleString", String.class), 0);

            assertThat(resolver.supportsParameter(param)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveArgument")
    class ResolveArgument {

        @Test
        @DisplayName("returns principal from request attribute (Requirement 6.5 - no re-resolution)")
        void returnsPrincipalFromAttribute() throws Exception {
            AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(
                    UserId.generate(), UserRole.PROJECT_ADMIN, "correlation-123");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute(IdentityFilter.PRINCIPAL_ATTRIBUTE, principal);
            NativeWebRequest webRequest = new ServletWebRequest(request);

            MethodParameter param = new MethodParameter(
                    SampleController.class.getMethod("handle", AuthenticatedPrincipal.class), 0);

            Object resolved = resolver.resolveArgument(param, null, webRequest, null);

            assertThat(resolved).isSameAs(principal);
        }

        @Test
        @DisplayName("throws IllegalStateException when attribute is null")
        void throwsWhenAttributeNull() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            NativeWebRequest webRequest = new ServletWebRequest(request);

            MethodParameter param = new MethodParameter(
                    SampleController.class.getMethod("handle", AuthenticatedPrincipal.class), 0);

            assertThatThrownBy(() -> resolver.resolveArgument(param, null, webRequest, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AuthenticatedPrincipal not found");
        }
    }

    // Sample controller class for MethodParameter extraction
    @SuppressWarnings("unused")
    static class SampleController {
        public void handle(AuthenticatedPrincipal principal) {}
        public void handleString(String value) {}
    }
}
