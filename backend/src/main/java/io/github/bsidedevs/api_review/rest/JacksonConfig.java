package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit Jackson configuration.
 *
 * <p>Spring Boot 4.x modularised auto-configuration: {@code spring-boot-starter-webmvc}
 * no longer transitively pulls in {@code spring-boot-starter-json}, so the
 * {@link ObjectMapper} bean is not guaranteed to be present unless either
 * {@code spring-boot-starter-json} is on the classpath or this bean is declared
 * explicitly.
 *
 * <p>{@link ConditionalOnMissingBean} ensures this is a pure fallback: if
 * {@code JacksonAutoConfiguration} fires (e.g. once {@code starter-json} is resolved
 * from the Maven cache), it takes precedence and this bean is skipped.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
