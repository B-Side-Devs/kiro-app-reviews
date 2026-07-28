package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.iam.PrincipalProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link IdentityFilter} with Spring Boot's embedded servlet container.
 *
 * <p>Configuration choices:
 * <ul>
 *   <li>Order 1: ensures the identity filter runs after {@code CorrelationIdFilter}
 *       (order 0), so the correlation ID is available in MDC.</li>
 *   <li>URL pattern {@code /api/v1/*}: limits the filter to API paths,
 *       so actuator endpoints are not affected.</li>
 * </ul>
 */
@Configuration
public class IdentityFilterConfig {

    @Bean
    public FilterRegistrationBean<IdentityFilter> identityFilterRegistration(
            PrincipalProvider principalProvider,
            ObjectMapper objectMapper,
            ProblemFactory problemFactory) {

        FilterRegistrationBean<IdentityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdentityFilter(principalProvider, objectMapper, problemFactory));
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(1);
        registration.setName("identityFilter");
        return registration;
    }
}
