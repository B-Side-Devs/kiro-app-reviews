package io.github.bsidedevs.api_review.iam;

import io.github.bsidedevs.api_review.shared.AccessError;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated principal from the HTTP header {@code X-User-Id}.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Exactly one header value must be present (Requirement 6.3, 6.9)</li>
 *   <li>Header value length must be ≤ 64 characters after trimming (Requirement 6.4)</li>
 *   <li>User ID must parse as a valid UUID v7 (Requirement 6.2)</li>
 *   <li>User must exist in the repository (Requirement 6.4)</li>
 * </ul>
 *
 * <p>All failure paths return {@link AccessError#UNAUTHENTICATED} without
 * revealing whether the user ID exists (Requirement 6.11).
 *
 * <p>If the {@link UserRepository} is unavailable (throws any exception),
 * the result is also {@link AccessError#UNAUTHENTICATED} (Requirement 6.10).
 *
 * <p>Role in the facade DAG: this is the implementation of {@link PrincipalProvider}
 * for development. The production authentication filter (Etapa 2) will replace
 * this with a Spring Security integration without changing the contract.
 *
 * <p>See {@code design.md} → section <em>Componente D: Proveedor_De_Principal</em>.
 */
@Component
public class HeaderPrincipalProvider implements PrincipalProvider {

    private final UserRepository userRepository;

    public HeaderPrincipalProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Result<AuthenticatedPrincipal, AccessError> resolve(List<String> headerValues, String correlationId) {
        // Requirement 6.3 (missing) and 6.9 (multiple values)
        if (headerValues == null || headerValues.size() != 1) {
            return Result.err(AccessError.UNAUTHENTICATED);
        }

        String rawValue = headerValues.get(0);
        if (rawValue == null) {
            return Result.err(AccessError.UNAUTHENTICATED);
        }

        // Requirement 6.3: empty after trim
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return Result.err(AccessError.UNAUTHENTICATED);
        }

        // Requirement 6.4: length exceeds maximum
        if (trimmed.length() > 64) {
            return Result.err(AccessError.UNAUTHENTICATED);
        }

        // Type-safe parsing of UserId (UUID v7 validation)
        UserId userId;
        try {
            userId = UserId.parse(trimmed);
        } catch (Exception e) {
            return Result.err(AccessError.UNAUTHENTICATED);
        }

        // Requirement 6.4: unknown user, Requirement 6.10: repository unavailable
        try {
            Optional<User> userOpt = userRepository.findById(userId.getValue());
            if (userOpt.isEmpty()) {
                return Result.err(AccessError.UNAUTHENTICATED);
            }

            User user = userOpt.get();
            return Result.ok(new AuthenticatedPrincipal(userId, user.getRole(), correlationId));
        } catch (Exception e) {
            // Requirement 6.10: if UserRepository is unavailable → UNAUTHENTICATED
            return Result.err(AccessError.UNAUTHENTICATED);
        }
    }
}
