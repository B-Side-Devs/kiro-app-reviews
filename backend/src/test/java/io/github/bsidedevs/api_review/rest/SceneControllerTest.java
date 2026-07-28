package io.github.bsidedevs.api_review.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bsidedevs.api_review.art.SceneFacade;
import io.github.bsidedevs.api_review.art.SceneFacade.SceneView;
import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.shared.DomainError;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionId;
import io.github.bsidedevs.api_review.shared.UserId;
import io.github.bsidedevs.api_review.shared.UserRole;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link SceneController}.
 *
 * <p>Validates that the controller correctly deserializes the request body as a
 * raw {@code String}, validates JSON syntax via {@link ObjectMapper#readTree},
 * and delegates to the facade with the correct arguments.
 */
class SceneControllerTest {

    private static final String VALID_JSON = "{\"type\":\"excalidraw\",\"version\":2,\"elements\":[]}";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private SceneFacade sceneFacade;
    private ProblemFactory problemFactory;
    private ObjectMapper objectMapper;
    private SceneController controller;

    @BeforeEach
    void setUp() {
        sceneFacade = mock(SceneFacade.class);
        problemFactory = new ProblemFactory();
        objectMapper = new ObjectMapper();
        controller = new SceneController(sceneFacade, problemFactory, objectMapper);
    }

    private AuthenticatedPrincipal principal() {
        return AuthenticatedPrincipal.of(UserId.parse("018e0c5a-7b3f-7000-8000-000000000001"),
                UserRole.PROJECT_ADMIN, "corr-test");
    }

    @Test
    @DisplayName("save with valid JSON body calls facade and returns 200")
    void saveWithValidJson() {
        var sessionId = ReviewSessionId.generate();
        var view = new SceneView(VALID_JSON, NOW, NOW);
        when(sceneFacade.saveScene(any(), any(), eq(VALID_JSON)))
                .thenReturn(Result.ok(view));

        ResponseEntity<?> response = controller.save(
                sessionId.getValue().toString(), VALID_JSON, principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo(VALID_JSON);
    }

    @Test
    @DisplayName("save with invalid JSON body returns 400")
    void saveWithInvalidJson() {
        var sessionId = ReviewSessionId.generate();

        ResponseEntity<?> response = controller.save(
                sessionId.getValue().toString(), "not valid json", principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        verify(sceneFacade, never()).saveScene(any(), any(), any());
    }

    @Test
    @DisplayName("save with blank body returns 400")
    void saveWithBlankBody() {
        var sessionId = ReviewSessionId.generate();

        ResponseEntity<?> response = controller.save(
                sessionId.getValue().toString(), "   ", principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(sceneFacade, never()).saveScene(any(), any(), any());
    }

    @Test
    @DisplayName("save with invalid session ID returns 400")
    void saveWithInvalidSessionId() {
        ResponseEntity<?> response = controller.save(
                "not-a-uuid", VALID_JSON, principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(sceneFacade, never()).saveScene(any(), any(), any());
    }

    @Test
    @DisplayName("save propagates facade error to error response")
    void savePropagatesFacadeError() {
        var sessionId = ReviewSessionId.generate();
        var error = DomainError.invalidTransition("SCENE_NOT_EDITABLE",
                "Scene can only be saved while RECORDING", null);
        when(sceneFacade.saveScene(any(), any(), eq(VALID_JSON)))
                .thenReturn(Result.err(error));

        ResponseEntity<?> response = controller.save(
                sessionId.getValue().toString(), VALID_JSON, principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("get returns 200 with stored document")
    void getReturnsDocument() {
        var sessionId = ReviewSessionId.generate();
        var view = new SceneView(VALID_JSON, NOW, NOW);
        when(sceneFacade.getScene(any(), any())).thenReturn(Result.ok(view));

        ResponseEntity<?> response = controller.get(
                sessionId.getValue().toString(), principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo(VALID_JSON);
    }

    @Test
    @DisplayName("save with null body returns 400")
    void saveWithNullBody() {
        var sessionId = ReviewSessionId.generate();

        ResponseEntity<?> response = controller.save(
                sessionId.getValue().toString(), null, principal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(sceneFacade, never()).saveScene(any(), any(), any());
    }
}
