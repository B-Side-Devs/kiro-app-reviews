/**
 * Domain package {@code rs} (Review Session — domain core).
 *
 * <p>Responsibility: manages the Review Session aggregate, its lifecycle
 * ({@code DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED → DELETED}) and the
 * valid transition matrix. Sole domain authorized to modify the {@code state} of a
 * Review Session. Also tracks {@code persistenceStatus} for isolated per-session
 * blocking on persistence failures.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code wspr} (for {@code canAccessProject}) and {@code iam}.</li>
 *   <li>Depended upon by: {@code art}, {@code notif}, {@code boff}.</li>
 * </ul>
 *
 * <p>Consumers interact with this domain exclusively through its public facade
 * (Spring beans). Internal classes are not visible to other domains.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.rs;
