package io.github.bsidedevs.api_review.observability;

import org.slf4j.MDC;

/**
 * Static utility for managing the correlation ID in the SLF4J MDC.
 *
 * <p>Used by {@link CorrelationIdFilter} to propagate the correlation ID
 * into structured logs for the duration of a request. The MDC key is
 * {@code correlationId}.
 *
 * <p>Thread-safety: MDC is inherently thread-local, so set/clear affect
 * only the calling thread.
 */
public final class CorrelationContext {

    /** MDC key under which the correlation ID is stored. */
    public static final String MDC_KEY = "correlationId";

    private CorrelationContext() {
        // Utility class
    }

    /**
     * Sets the correlation ID in the MDC.
     *
     * @param correlationId the correlation ID value (must not be null)
     */
    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    /**
     * Returns the current correlation ID from the MDC, or {@code null} if none is set.
     *
     * @return the correlation ID, or null
     */
    public static String get() {
        return MDC.get(MDC_KEY);
    }

    /**
     * Clears the correlation ID from the MDC.
     * Safe to call even if no value was previously set.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
