/**
 * Cross-cutting package {@code rest} (REST layer and OpenAPI publication).
 *
 * <p>Responsibility: exposes the domain facades as a REST API under {@code /api/v1}.
 * Contains HTTP controllers, the correlation / authentication / authorization filter
 * chain, {@code Problem+JSON} (RFC 7807) error serialization and OpenAPI configuration.
 * Publishes the OpenAPI specification via HTTP and keeps the previous specification
 * available if regeneration fails.
 *
 * <p>Role in the facade DAG: <strong>cross-cutting adapter</strong>. Translates HTTP
 * requests into invocations of the public facades of {@code iam}, {@code wspr},
 * {@code rs}, {@code art}, {@code notif} and {@code boff}. Must not access domain
 * internal classes. Does not appear inside the domain DAG.
 *
 * <p>Current state: only {@code ReviewSessionController} (create/get/list under
 * {@code /api/v1/review-sessions}) is implemented, using a plain {@code X-User-Id}
 * header instead of a resolved principal. The correlation, authentication and
 * authorization filter chain, {@code Problem+JSON} error mapping and OpenAPI
 * publication described below are not implemented yet.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}, {@code observability},
 * {@code security}.
 *
 * <p>See {@code design.md} → section <em>Componente 7: Capa REST</em>.
 */
package io.github.bsidedevs.api_review.rest;
