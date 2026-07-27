package io.github.bsidedevs.api_review.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link CorrelationIdFilter} with Spring Boot's embedded
 * servlet container.
 *
 * <p>Configuration choices:
 * <ul>
 *   <li>Order 0: ensures the correlation filter runs before all other
 *       application filters (IdentityFilter is order 1).</li>
 *   <li>URL pattern {@code /api/v1/*}: limits the filter to API paths,
 *       so actuator endpoints are not affected.</li>
 * </ul>
 */
@Configuration
public class CorrelationIdFilterConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(0);
        registration.setName("correlationIdFilter");
        return registration;
    }
}
