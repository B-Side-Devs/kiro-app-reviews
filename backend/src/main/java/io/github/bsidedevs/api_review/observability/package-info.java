/**
 * Cross-cutting package {@code observability} (structured logging, correlation, metrics).
 *
 * <p>Responsibility: guarantees every relevant operation produces structured JSON logs
 * with a {@code correlationId}, exposes metrics ({@code /actuator/prometheus} or
 * equivalent) and provides end-to-end traceability. Owns the correlation filter, the
 * MDC propagation utilities and the error-recording policy (primary sink to stdout with
 * fallback to a local file; if both fail, the original operation continues).
 *
 * <p>Role in the facade DAG: <strong>cross-cutting infrastructure</strong>. Consumed by
 * every domain and by {@code rest} and {@code security}. Must not depend on any domain
 * facade.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → section <em>Componente 8: Observability</em>.
 */
package io.github.bsidedevs.api_review.observability;
