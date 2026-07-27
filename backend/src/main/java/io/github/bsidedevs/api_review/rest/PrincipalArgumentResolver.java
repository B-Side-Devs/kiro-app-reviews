package io.github.bsidedevs.api_review.rest;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link AuthenticatedPrincipal} from the request attribute set by
 * {@link IdentityFilter}.
 *
 * <p>This resolver does NOT re-resolve the principal (Requirement 6.5).
 * It simply reads the already-resolved principal stored as a request attribute
 * during filter processing.
 *
 * <p>If the attribute is null (which should not happen since IdentityFilter
 * short-circuits on failure), an {@link IllegalStateException} is thrown.
 */
public class PrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException(
                    "No HttpServletRequest available in NativeWebRequest");
        }

        Object principal = request.getAttribute(IdentityFilter.PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new IllegalStateException(
                    "AuthenticatedPrincipal not found in request attributes. "
                            + "IdentityFilter should have short-circuited the request.");
        }

        return principal;
    }
}
