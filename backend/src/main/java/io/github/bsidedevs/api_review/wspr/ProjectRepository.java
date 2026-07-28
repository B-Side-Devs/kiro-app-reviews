package io.github.bsidedevs.api_review.wspr;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByWorkspaceId(UUID workspaceId);

    List<Project> findByOwnerAdminId(UUID ownerAdminId);
}
