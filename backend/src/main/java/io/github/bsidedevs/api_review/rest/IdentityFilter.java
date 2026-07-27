package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.iam.PrincipalProvider;
import io.github.bsidedevs.api_review.observability.CorrelationContext;
import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet filter that resolves the authenticated principal from the {@code X-User-Id}
 * header BEFORE request body deserialization.
 *
 * <p>Filter order: 1 (after {@code CorrelationIdFilter} which is order 0).
 * This guarantees that the correlation ID is available in MDC when identity
 * resolution occurs.
 *
 * <p>On successful resolution, the {@link AuthenticatedPrincipal} is stored as
 * a request attribute under the key {@link #PRINCIPAL_ATTRIBUTE} for downstream
 * consumption by {@link PrincipalArgumentResolver}.
 *
 * <p>On failure, the filter short-circuits the chain and writes a 401
 * Problem+JSON response directly, preventing any further processing.
 *
 * <p>URL pattern: {@code /api/v1/*} — applied only to API paths so that
 * actuator endpoints are not affected.
 *
 * @see PrincipalArgumentResolver
 * @see IdentityFilterConfig
 */
public class IdentityFilter extends OncePerRequestFilter {

    /** Header name for the user identity. */
    public static final String HEADER = "X-User-Id";

    /** Request attribute key under which the resolved principal is stored. */
    public static final String PRINCIPAL_ATTRIBUTE = "authenticatedPrincipal";

    private final PrincipalProvider principalProvider;
    private final ObjectMapper objectMapper;
    private final ProblemFactory problemFactory;

    public IdentityFilter(PrincipalProvider principalProvider,
                          ObjectMapper objectMapper,
                          ProblemFactory problemFactory) {
        this.principalProvider = principalProvider;
        this.objectMapper = objectMapper;
        this.problemFactory = problemFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Get ALL values of the X-User-Id header
        List<String> headerValues = Collections.list(request.getHeaders(HEADER));

        // Get correlationId from CorrelationContext (already set by CorrelationIdFilter)
        String correlationId = CorrelationContext.get();

        // Resolve principal via PrincipalProvider
        Result<AuthenticatedPrincipal, ?> result = principalProvider.resolve(headerValues, correlationId);

        if (result.isOk()) {
            // Store principal as request attribute and continue the filter chain
            request.setAttribute(PRINCIPAL_ATTRIBUTE, result.unwrap());
            filterChain.doFilter(request, response);
        } else {
            // Short-circuit: write 401 Problem+JSON response
            writeUnauthorizedResponse(response, correlationId);
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String correlationId)
            throws IOException {

        DomainError error = DomainError.unauthenticated("UNAUTHENTICATED", "Authentication required");
        ResponseEntity<ReviewSessionDto.ProblemResponse> entity =
                problemFactory.toResponseEntity(error, correlationId);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getOutputStream(), entity.getBody());
    }
}
