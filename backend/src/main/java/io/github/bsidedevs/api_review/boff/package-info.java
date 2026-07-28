/**
 * Domain package {@code boff} (Backoffice). Not implemented yet — scaffolding only.
 *
 * <p>Responsibility: will expose administrative queries to the {@code PLATFORM_ADMIN}
 * (counters, user listings, platform metrics), explicitly never exposing Review
 * Session content (recordings, comments, notes, AI artifacts).
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code iam}, {@code wspr}, {@code rs} (aggregate counters only,
 *       never content).</li>
 *   <li>Depended upon by: none (leaf of the domain DAG).</li>
 * </ul>
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.boff;
