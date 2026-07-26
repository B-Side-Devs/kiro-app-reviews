/**
 * Domain package {@code art} (Artifacts).
 *
 * <p>Responsibility: manages every artifact produced inside a Review Session:
 * rrweb recordings, DOM snapshots, timeline events, browser metadata, Excalidraw
 * scenes, comments, text notes, voice notes, transcriptions and AI-generated
 * artifacts. Enforces immutable authorship and creation timestamps, and the
 * author-only rule for modifying / deleting annotations.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code rs} (session state / access) and {@code iam}.</li>
 *   <li>Depended upon by: {@code notif}.</li>
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
package io.github.bsidedevs.api_review.art;
