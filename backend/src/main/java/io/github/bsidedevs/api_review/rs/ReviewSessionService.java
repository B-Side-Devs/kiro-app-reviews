package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.iam.UserRepository;
import io.github.bsidedevs.api_review.shared.Result;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import io.github.bsidedevs.api_review.wspr.Project;
import io.github.bsidedevs.api_review.wspr.ProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the Review Session aggregate.
 *
 * <p>MVP authorization: a Review Session can only be created/read/listed by the
 * owning Project Admin. The full role+relationship model (wspr facade) lands in
 * a later spec; this trusts the {@code X-User-Id} header against the seeded user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSessionService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public Result<ReviewSession, String> createSession(UUID projectId, UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return Result.err("USER_NOT_FOUND");
        }

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return Result.err("PROJECT_NOT_FOUND");
        }
        if (!projectOpt.get().isOwnedBy(userId)) {
            return Result.err("NOT_AUTHORIZED");
        }

        ReviewSession session = new ReviewSession(UUID.randomUUID(), projectId, userId);
        ReviewSession saved = reviewSessionRepository.save(session);
        log.info("Created review session {} for project {}", saved.getReviewSessionId(), projectId);
        return Result.ok(saved);
    }

    @Transactional(readOnly = true)
    public Result<ReviewSession, String> getSession(UUID sessionId, UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return Result.err("USER_NOT_FOUND");
        }

        Optional<ReviewSession> sessionOpt = reviewSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Result.err("SESSION_NOT_FOUND");
        }

        ReviewSession session = sessionOpt.get();
        Optional<Project> projectOpt = projectRepository.findById(session.getProjectId());
        if (projectOpt.isEmpty() || !projectOpt.get().isOwnedBy(userId)) {
            return Result.err("NOT_AUTHORIZED");
        }

        return Result.ok(session);
    }

    @Transactional(readOnly = true)
    public Result<List<ReviewSession>, String> listSessions(UUID projectId, UUID userId, boolean activeOnly) {
        if (userRepository.findById(userId).isEmpty()) {
            return Result.err("USER_NOT_FOUND");
        }

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty() || !projectOpt.get().isOwnedBy(userId)) {
            return Result.err("NOT_AUTHORIZED");
        }

        List<ReviewSession> sessions = activeOnly
                ? reviewSessionRepository.findByProjectIdAndStateNotIn(
                        projectId, List.of(ReviewSessionState.ARCHIVED, ReviewSessionState.DELETED))
                : reviewSessionRepository.findByProjectId(projectId);

        return Result.ok(sessions);
    }
}
