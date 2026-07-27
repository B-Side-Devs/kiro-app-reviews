package io.github.bsidedevs.api_review.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.iam.PrincipalProvider;
import io.github.bsidedevs.api_review.observability.CorrelationContext;
import io.github.bsidedevs.api_review.shared.AccessError;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("IdentityFilter")
class IdentityFilterTest {

    private PrincipalProvider principalProvider;
    private ObjectMapper objectMapper;
    private IdentityFilter filter;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String CORRELATION_ID = "test-correlation-123";

    @BeforeEach
    void setUp() {
        principalProvider = mock(PrincipalProvider.class);
        objectMapper = new ObjectMapper();
        filter = new IdentityFilter(principalProvider, objectMapper, new ProblemFactory());
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // CorrelationIdFilter runs before IdentityFilter, so the MDC should be set
        CorrelationContext.set(CORRELATION_ID);
    }

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
    }

    @Nested
    @DisplayName("on successful resolution")
    class SuccessfulResolution {

        @Test
        @DisplayName("stores principal as request attribute and continues chain (Requirement 6.5)")
        void storesPrincipalAndContinuesChain() throws Exception {
            UserId userId = UserId.generate();
            AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(
                    userId, UserRole.PROJECT_ADMIN, CORRELATION_ID);

            request.addHeader("X-User-Id", userId.getValue().toString());
            when(principalProvider.resolve(anyList(), anyString())).thenReturn(Result.ok(principal));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(request.getAttribute(IdentityFilter.PRINCIPAL_ATTRIBUTE)).isSameAs(principal);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("passes header values to PrincipalProvider")
        void passesHeaderValues() throws Exception {
            UserId userId = UserId.generate();
            String userIdStr = userId.getValue().toString();
            AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(
                    userId, UserRole.CLIENT, CORRELATION_ID);

            request.addHeader("X-User-Id", userIdStr);
            when(principalProvider.resolve(List.of(userIdStr), CORRELATION_ID))
                    .thenReturn(Result.ok(principal));

            filter.doFilterInternal(request, response, filterChain);

            verify(principalProvider).resolve(List.of(userIdStr), CORRELATION_ID);
        }

        @Test
        @DisplayName("passes correlationId from CorrelationContext")
        void passesCorrelationId() throws Exception {
            UserId userId = UserId.generate();
            AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(
                    userId, UserRole.PROJECT_ADMIN, CORRELATION_ID);

            request.addHeader("X-User-Id", userId.getValue().toString());
            when(principalProvider.resolve(anyList(), anyString())).thenReturn(Result.ok(principal));

            filter.doFilterInternal(request, response, filterChain);

            verify(principalProvider).resolve(anyList(), org.mockito.Mockito.eq(CORRELATION_ID));
        }
    }

    @Nested
    @DisplayName("on failed resolution")
    class FailedResolution {

        @Test
        @DisplayName("returns 401 status (Requirement 6.3)")
        void returns401() throws Exception {
            when(principalProvider.resolve(anyList(), anyString()))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("writes application/problem+json content type")
        void writesProblemJson() throws Exception {
            when(principalProvider.resolve(anyList(), anyString()))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentType()).isEqualTo("application/problem+json");
        }

        @Test
        @DisplayName("does not continue the filter chain (Requirement 6.6)")
        void shortCircuitsChain() throws Exception {
            when(principalProvider.resolve(anyList(), anyString()))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("does not set principal attribute")
        void noPrincipalAttribute() throws Exception {
            when(principalProvider.resolve(anyList(), anyString()))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(request.getAttribute(IdentityFilter.PRINCIPAL_ATTRIBUTE)).isNull();
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("response body contains Problem+JSON with correlationId")
        void responseBodyContainsProblemJson() throws Exception {
            when(principalProvider.resolve(anyList(), anyString()))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            String body = response.getContentAsString();
            Map<String, Object> problem = objectMapper.readValue(body, Map.class);

            assertThat(problem.get("type")).isEqualTo("about:blank");
            assertThat(problem.get("title")).isEqualTo("Unauthorized");
            assertThat(problem.get("status")).isEqualTo(401);
            assertThat(problem.get("detail")).isEqualTo("Authentication required");
            assertThat(problem.get("instance")).isEqualTo("/correlation/" + CORRELATION_ID);
            assertThat(problem.get("code")).isEqualTo("UNAUTHENTICATED");
        }

        @Test
        @DisplayName("passes empty list when no X-User-Id header present")
        void noHeaderPassesEmptyList() throws Exception {
            when(principalProvider.resolve(Collections.emptyList(), CORRELATION_ID))
                    .thenReturn(Result.err(AccessError.UNAUTHENTICATED));

            filter.doFilterInternal(request, response, filterChain);

            verify(principalProvider).resolve(Collections.emptyList(), CORRELATION_ID);
        }
    }
}
