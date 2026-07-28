package io.github.bsidedevs.api_review.wspr;

import io.github.bsidedevs.api_review.shared.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing an invitation granting a Client access to a Project.
 * Maps to the {@code invitations} table created in V6.
 */
@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    @Id
    @Column(name = "invitation_id")
    private UUID invitationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "invitee_user_id", nullable = false)
    private UUID inviteeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvitationStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public Invitation(UUID invitationId, UUID projectId, UUID inviteeUserId,
                      InvitationStatus status, Instant issuedAt) {
        this.invitationId = invitationId;
        this.projectId = projectId;
        this.inviteeUserId = inviteeUserId;
        this.status = status;
        this.issuedAt = issuedAt;
    }
}
