/**
 * Domain package {@code art} (Artifacts).
 *
 * <p>Responsibility: will manage every artifact produced inside a Review Session
 * (rrweb recordings, DOM snapshots, timeline events, browser metadata, Excalidraw
 * scenes, comments, text notes, voice notes, transcriptions, AI-generated results),
 * enforcing immutable authorship/timestamps and author-only edit/delete of annotations.
 *
 * <p>Current state: only the Excalidraw {@code Scene} is implemented (MVP). A Scene
 * is persisted 1:1 with a Review Session as an <em>opaque</em> JSON document in the
 * official Excalidraw public format; the backend never interprets its structure.
 * Saves are upserts (full replacement, no history) and are allowed only while the
 * session is {@code RECORDING}. Exposed through {@link io.github.bsidedevs.api_review.art.SceneFacade};
 * authorization and session state are delegated to the {@code rs} facade. The broader
 * generic {@code Artifact} aggregate and the chunked upload protocol remain future work.
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
