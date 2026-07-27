/**
 * Domain package {@code art} (Artifacts). Not implemented yet — scaffolding only.
 *
 * <p>Responsibility: will manage every artifact produced inside a Review Session
 * (rrweb recordings, DOM snapshots, timeline events, browser metadata, Excalidraw
 * scenes, comments, text notes, voice notes, transcriptions, AI-generated results),
 * enforcing immutable authorship/timestamps and author-only edit/delete of annotations.
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code rs} (session state / access) and {@code iam}.</li>
 *   <li>Depended upon by: {@code notif}.</li>
 * </ul>
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.art;
