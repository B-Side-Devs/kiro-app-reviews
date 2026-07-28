package io.github.bsidedevs.api_review.wspr;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the {@link Invitation} entity.
 * Provides lookup by project and invitee for authorization checks.
 */
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    /**
     * Finds all invitations for a given project and invitee user.
     *
     * @param projectId the project identifier
     * @param inviteeUserId the invitee user identifier
     * @return list of invitations (may be empty)
     */
    List<Invitation> findByProjectIdAndInviteeUserId(UUID projectId, UUID inviteeUserId);
}
