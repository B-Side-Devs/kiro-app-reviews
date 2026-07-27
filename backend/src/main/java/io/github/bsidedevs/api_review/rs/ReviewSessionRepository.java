package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, UUID> {

    List<ReviewSession> findByProjectId(UUID projectId);

    List<ReviewSession> findByProjectIdAndStateNotIn(UUID projectId, List<ReviewSessionState> states);
}
