package io.github.bsidedevs.api_review.iam;

import io.github.bsidedevs.api_review.shared.UserRole;
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

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String email;

    private String displayName;

    private Instant createdAt;

    public User(UUID userId, String email, UserRole role, String displayName) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }
}
