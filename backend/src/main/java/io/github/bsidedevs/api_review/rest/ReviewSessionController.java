package io.github.bsidedevs.api_review.rest;

import io.github.bsidedevs.api_review.rest.dto.ReviewSessionDto;
import io.github.bsidedevs.api_review.rs.ReviewSession;
import io.github.bsidedevs.api_review.rs.ReviewSessionService;
import io.github.bsidedevs.api_review.shared.Result;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review-sessions")
@RequiredArgsConstructor
public class ReviewSessionController {

    private final ReviewSessionService reviewSessionService;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody ReviewSessionDto.CreateRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        Result<ReviewSession, String> result =
                reviewSessionService.createSession(request.projectId(), userId);

        return result.isOk()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ReviewSessionDto.Response.from(result.unwrap()))
                : toErrorResponse(result.unwrapErr());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        Result<ReviewSession, String> result = reviewSessionService.getSession(id, userId);

        return result.isOk()
                ? ResponseEntity.ok(ReviewSessionDto.Response.from(result.unwrap()))
                : toErrorResponse(result.unwrapErr());
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestHeader("X-User-Id") UUID userId) {

        Result<List<ReviewSession>, String> result =
                reviewSessionService.listSessions(projectId, userId, activeOnly);

        return result.isOk()
                ? ResponseEntity.ok(ReviewSessionDto.ListResponse.from(result.unwrap()))
                : toErrorResponse(result.unwrapErr());
    }

    private ResponseEntity<ReviewSessionDto.ErrorResponse> toErrorResponse(String error) {
        HttpStatus status = switch (error) {
            case "USER_NOT_FOUND" -> HttpStatus.UNAUTHORIZED;
            case "PROJECT_NOT_FOUND", "SESSION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "NOT_AUTHORIZED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(new ReviewSessionDto.ErrorResponse(error, error));
    }
}
