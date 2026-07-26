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
@Table(name = "workspaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace {

    @Id
    private UUID workspaceId;

    private UUID ownerAdminId;

    private String displayName;

    private Instant createdAt;

    public Workspace(UUID workspaceId, UUID ownerAdminId, String displayName) {
        this.workspaceId = workspaceId;
        this.ownerAdminId = ownerAdminId;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }
}
