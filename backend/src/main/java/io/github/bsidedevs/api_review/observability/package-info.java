/**
 * Cross-cutting package {@code observability} (structured logging, correlation,
 * metrics). Not implemented yet — scaffolding only.
 *
 * <p>Responsibility: will guarantee every relevant operation produces structured JSON
 * logs with a {@code correlationId}, expose metrics ({@code /actuator/prometheus} or
 * equivalent), and own the correlation filter, MDC propagation and the error-recording
 * policy (primary sink to stdout, fallback to a local file, continue on double failure).
 *
 * <p>Role in the facade DAG: <strong>cross-cutting infrastructure</strong>. Will be
 * consumed by every domain and by {@code rest} and {@code security}; must not depend
 * on any domain facade.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → section <em>Componente 8: Observability</em>.
 */
package io.github.bsidedevs.api_review.observability;
