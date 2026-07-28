package io.github.bsidedevs.api_review.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.art.SceneFacade;
import io.github.bsidedevs.api_review.art.SceneFacade.SceneView;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.observability.CorrelationContext;
import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Excalidraw Scene of a Review Session.
 *
 * <p>Routes under {@code /api/v1/review-sessions/{sessionId}/scene}:
 * <ul>
 *   <li>{@code PUT} — save (upsert) the Scene; allowed only while RECORDING</li>
 *   <li>{@code GET} — retrieve the last stored Scene verbatim</li>
 * </ul>
 *
 * <p>The request/response body is the opaque Excalidraw JSON document. The
 * backend never interprets its structure; Jackson only guarantees it is
 * syntactically valid JSON. Authorization and Review Session semantics are
 * handled by the {@link SceneFacade} (which delegates to the {@code rs} facade).
 */
@RestController
@RequiredArgsConstructor
public class SceneController {

    private final SceneFacade sceneFacade;
    private final ProblemFactory problemFactory;
    private final ObjectMapper objectMapper;

    @PutMapping(
            path = "/api/v1/review-sessions/{sessionId}/scene",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(
            @PathVariable String sessionId,
            @RequestBody String body,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        if (body == null || body.isBlank()) {
            return toValidationErrorResponse("document", "Scene document must not be empty");
        }

        // Validate that the body is syntactically correct JSON
        try {
            objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return toValidationErrorResponse("document", "Scene document is not valid JSON");
        }

        Result<SceneView, DomainError> result =
                sceneFacade.saveScene(principal, parsedSessionId, body);

        if (result.isOk()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.unwrap().document());
        }
        return toErrorResponse(result.unwrapErr());
    }

    @GetMapping(
            path = "/api/v1/review-sessions/{sessionId}/scene",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(
            @PathVariable String sessionId,
            AuthenticatedPrincipal principal) {

        ReviewSessionId parsedSessionId = parseSessionId(sessionId);
        if (parsedSessionId == null) {
            return toValidationErrorResponse("sessionId", "Invalid session ID format: must be a valid UUID v7");
        }

        Result<SceneView, DomainError> result =
                sceneFacade.getScene(principal, parsedSessionId);

        if (result.isOk()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.unwrap().document());
        }
        return toErrorResponse(result.unwrapErr());
    }

    // ---- Private helpers (mirrors ReviewSessionController) ----

    private ReviewSessionId parseSessionId(String sessionId) {
        try {
            return ReviewSessionId.parse(sessionId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ResponseEntity<ReviewSessionDto.ProblemResponse> toErrorResponse(DomainError error) {
        return problemFactory.toResponseEntity(error, CorrelationContext.get());
    }

    private ResponseEntity<ReviewSessionDto.ProblemResponse> toValidationErrorResponse(String field, String message) {
        DomainError error = DomainError.validation(
                "INVALID_REQUEST",
                message,
                List.of(new DomainError.FieldError(field, message)));
        return problemFactory.toResponseEntity(error, CorrelationContext.get());
    }
}
