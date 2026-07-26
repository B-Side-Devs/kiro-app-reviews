/**
 * Domain package {@code notif} (Notifications).
 *
 * <p>Responsibility: produces notifications on relevant domain events
 * ({@code COMMENT_ADDED}, {@code COMMENT_REPLIED}, {@code SESSION_COMPLETED},
 * {@code AI_PROCESSING_COMPLETED}) and records them in a per-user history queryable
 * by the recipient. Applies deterministic event → recipient mapping with the
 * one-single-notification consolidation rule when the reply author equals the
 * project-admin owner. Retries history recording up to 3 total attempts.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code rs}, {@code art}, {@code wspr}, {@code iam}.</li>
 *   <li>Depended upon by: none (leaf of the domain DAG).</li>
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
package io.github.bsidedevs.api_review.notif;
