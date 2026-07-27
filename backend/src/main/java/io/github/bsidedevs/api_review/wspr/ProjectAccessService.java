package io.github.bsidedevs.api_review.wspr;

import io.github.bsidedevs.api_review.iam.AuthenticatedPrincipal;
import io.github.bsidedevs.api_review.shared.AccessDecision;
import io.github.bsidedevs.api_review.shared.AccessReason;
import io.github.bsidedevs.api_review.shared.ProjectId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link WorkspaceProjectFacade} that evaluates project access
 * with a 2000ms timeout budget (Requirement 5.6).
 *
 * <p>Delegates the actual authorization logic to {@link AccessDecisionEvaluator}
 * and wraps the evaluation in a timeout mechanism. If the evaluation times out
 * or throws any exception, returns {@code AccessDecision.denied(DENIED_UNKNOWN)}
 * following the fail-closed principle.
 */
@Service
public class ProjectAccessService implements WorkspaceProjectFacade {

    private static final Logger log = LoggerFactory.getLogger(ProjectAccessService.class);
    private static final long TIMEOUT_MS = 2000;

    private final AccessDecisionEvaluator evaluator;
    private final ExecutorService accessDecisionExecutor;

    public ProjectAccessService(AccessDecisionEvaluator evaluator) {
        this.evaluator = evaluator;
        this.accessDecisionExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "access-decision-executor");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public AccessDecision canAccessProject(AuthenticatedPrincipal principal, ProjectId projectId) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> evaluator.evaluateDecision(principal, projectId),
                    accessDecisionExecutor
            )
            .orTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .join();
        } catch (Exception e) {
            log.warn("Access decision evaluation failed or timed out for principal={}, project={}: {}",
                    principal != null ? principal.userId() : "null",
                    projectId,
                    e.getMessage());
            return AccessDecision.denied(AccessReason.DENIED_UNKNOWN);
        }
    }
}
