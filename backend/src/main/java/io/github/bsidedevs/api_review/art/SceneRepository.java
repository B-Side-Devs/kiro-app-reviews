package io.github.bsidedevs.api_review.art;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Scene} aggregates, keyed by the owning Review Session id
 * (1:1). A missing row means the Review Session has no persisted Scene yet.
 */
@Repository
public interface SceneRepository extends JpaRepository<Scene, UUID> {
}
