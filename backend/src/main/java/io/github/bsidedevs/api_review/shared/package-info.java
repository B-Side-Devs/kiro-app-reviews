/**
 * Cross-cutting package {@code shared} (opaque identifiers and primitive types).
 *
 * <p>Responsibility: hosts opaque identifiers ({@code UserId}, {@code WorkspaceId},
 * {@code ProjectId}, {@code ReviewSessionId}, {@code ArtifactId}, {@code InvitationId},
 * {@code NotificationId}, {@code SessionId}, {@code CorrelationId}), value types
 * ({@code Instant}, {@code EmailAddress}, {@code PayloadRef}), functional utilities
 * ({@code Result}, {@code AccessError}) and shared enums ({@code UserRole},
 * {@code ReviewSessionState}, {@code InvitationStatus}, {@code ArtifactKind},
 * {@code NotificationKind}, {@code DeliveryStatus}, {@code PersistenceStatus},
 * {@code AccessReason}).
 *
 * <p>Role in the facade DAG: <strong>importable by any domain or cross-cutting
 * package</strong>. Does not depend on any domain. It is the only package that every
 * other package may freely reference.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.shared;
