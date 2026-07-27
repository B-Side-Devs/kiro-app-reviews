package io.github.bsidedevs.api_review.wspr;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    private UUID projectId;

    private UUID workspaceId;

    private UUID ownerAdminId;

    private String displayName;

    private Instant createdAt;

    public Project(UUID projectId, UUID workspaceId, UUID ownerAdminId, String displayName) {
        this.projectId = projectId;
        this.workspaceId = workspaceId;
        this.ownerAdminId = ownerAdminId;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerAdminId.equals(userId);
    }
}
