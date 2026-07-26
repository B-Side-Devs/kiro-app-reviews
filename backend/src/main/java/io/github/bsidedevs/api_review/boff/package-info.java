/**
 * Domain package {@code boff} (Backoffice).
 *
 * <p>Responsibility: exposes administrative queries to the {@code PLATFORM_ADMIN}
 * (counters, listings of users and platform metrics). Explicitly does <strong>not</strong>
 * expose access to Review Session content (recordings, comments, notes, AI artifacts).
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code iam}, {@code wspr}, {@code rs} (for aggregate counters only,
 *       never for content).</li>
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
package io.github.bsidedevs.api_review.boff;
