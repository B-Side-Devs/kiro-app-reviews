/**
 * Cross-cutting package {@code shared} (opaque identifiers and primitive types).
 *
 * <p>Hosts opaque identifiers ({@code UserId}, {@code WorkspaceId}, {@code ProjectId},
 * {@code ReviewSessionId}, {@code ArtifactId}, {@code InvitationId},
 * {@code NotificationId}, {@code SessionId}, {@code CorrelationId}), value types
 * ({@code EmailAddress}, {@code PayloadRef}), the functional {@code Result} /
 * {@code AccessError} types and shared enums ({@code UserRole},
 * {@code ReviewSessionState}, {@code InvitationStatus}, {@code ArtifactKind},
 * {@code NotificationKind}, {@code DeliveryStatus}, {@code PersistenceStatus},
 * {@code AccessReason}). Fully implemented, including unit tests.
 *
 * <p>Role in the facade DAG: <strong>importable by any domain or cross-cutting
 * package</strong>. Depends on nothing else; the only package every other package
 * may freely reference.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.shared;
