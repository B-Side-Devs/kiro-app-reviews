package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, UUID> {

    List<ReviewSession> findByProjectId(UUID projectId);

    List<ReviewSession> findByProjectIdAndStateNotIn(UUID projectId, List<ReviewSessionState> states);

    /**
     * Finds review sessions for listing with scope filtering, fixed ordering, and a cap of 200.
     *
     * <p>Ordering: {@code created_at DESC}, then {@code review_session_id ASC} as tiebreaker.
     * Pass {@code PageRequest.of(0, 200)} as the {@code pageable} argument to enforce the cap.
     *
     * <p>Scope semantics (Requirements 8.4–8.5):
     * <ul>
     *   <li>ACTIVE ({@link ReviewSessionFacade.ListingScope#ACTIVE}): excludes ARCHIVED and DELETED.</li>
     *   <li>ALL ({@link ReviewSessionFacade.ListingScope#ALL}): excludes only DELETED.</li>
     * </ul>
     *
     * @param projectId the project to list sessions for
     * @param excludedStates states to exclude from results
     * @param pageable pagination/ordering — use {@code PageRequest.of(0, 200, Sort.unsorted())}
     *                 (the JPQL ORDER BY clause already specifies the sort)
     * @return up to 200 sessions ordered by created_at DESC, review_session_id ASC
     */
    @Query("SELECT rs FROM ReviewSession rs "
            + "WHERE rs.projectId = :projectId "
            + "AND rs.state NOT IN :excludedStates "
            + "ORDER BY rs.createdAt DESC, rs.reviewSessionId ASC")
    List<ReviewSession> findForListing(
            @Param("projectId") UUID projectId,
            @Param("excludedStates") List<ReviewSessionState> excludedStates,
            Pageable pageable);
}
