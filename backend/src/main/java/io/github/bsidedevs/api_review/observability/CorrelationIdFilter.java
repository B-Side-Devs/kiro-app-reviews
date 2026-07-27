package io.github.bsidedevs.api_review.observability;

import io.github.bsidedevs.api_review.shared.UuidV7Generator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that ensures every API response contains exactly one
 * {@code X-Correlation-Id} header and that the correlation ID is available
 * in the SLF4J MDC for structured logging.
 *
 * <p>Filter order: 0 (first in the chain). This guarantees that the
 * correlation ID is available for all subsequent filters and error handlers,
 * including {@code IdentityFilter} and {@code RestExceptionHandler}.
 *
 * <p>Behavior:
 * <ul>
 *   <li>If the request contains a valid {@code X-Correlation-Id} header
 *       (1–128 characters), it is reused as-is (Requirement 10.13).</li>
 *   <li>Otherwise, a new UUID v7 is generated (Requirements 10.14, 10.18).</li>
 *   <li>The value is set on the response via {@code setHeader} (not
 *       {@code addHeader}) to guarantee exactly one occurrence (Requirement 10.12).</li>
 *   <li>The value is placed in MDC before the filter chain executes, so it
 *       accompanies even error responses (Requirements 10.15, 10.17).</li>
 *   <li>MDC is always cleared in a finally block (Requirement 10.16).</li>
 * </ul>
 *
 * <p>URL pattern: {@code /api/v1/*} — applied only to API paths so that
 * actuator endpoints ({@code /actuator/health}) are not affected.
 *
 * @see CorrelationContext
 * @see CorrelationIdFilterConfig
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Header name for the correlation ID. */
    public static final String HEADER = "X-Correlation-Id";

    /** Maximum allowed length for an inbound correlation ID. */
    public static final int MAX_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String inbound = request.getHeader(HEADER);

        String correlationId;
        if (inbound != null && !inbound.isEmpty() && inbound.length() <= MAX_LENGTH) {
            correlationId = inbound;
        } else {
            correlationId = UuidV7Generator.generate().toString();
        }

        // Set on response BEFORE chain so it accompanies error responses (Req 10.12, 10.17)
        response.setHeader(HEADER, correlationId);

        // Set in MDC for structured logging (Req 10.15)
        CorrelationContext.set(correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC (Req 10.16)
            CorrelationContext.clear();
        }
    }
}
