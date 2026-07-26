/**
 * Domain package {@code notif} (Notifications). Not implemented yet — scaffolding only.
 *
 * <p>Responsibility: will produce notifications on relevant domain events
 * ({@code COMMENT_ADDED}, {@code COMMENT_REPLIED}, {@code SESSION_COMPLETED},
 * {@code AI_PROCESSING_COMPLETED}) and record them in a per-recipient history, with
 * deterministic event → recipient mapping, single-notification consolidation when the
 * reply author equals the project-admin owner, and retry of history recording up to
 * 3 total attempts.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code rs}, {@code art}, {@code wspr}, {@code iam}.</li>
 *   <li>Depended upon by: none (leaf of the domain DAG).</li>
 * </ul>
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.notif;
