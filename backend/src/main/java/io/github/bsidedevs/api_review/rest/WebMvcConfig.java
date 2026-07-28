package io.github.bsidedevs.api_review.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration that registers custom argument resolvers.
 *
 * <p>Registers the {@link PrincipalArgumentResolver} so that controller methods
 * can receive the {@link io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal}
 * directly as a method parameter without manual extraction from request attributes.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PrincipalArgumentResolver());
    }
}
