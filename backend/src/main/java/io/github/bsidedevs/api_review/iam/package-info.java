/**
 * Domain package {@code iam} (Identity &amp; Access Management).
 *
 * <p>Responsibility: manages user identities ({@code PROJECT_ADMIN}, {@code CLIENT},
 * {@code PLATFORM_ADMIN}), credentials, sessions, invitation tokens and low-level
 * role-based authorization decisions.
 *
 * <p>Role in the facade DAG: this domain is the <strong>root</strong> of the dependency
 * graph. It does <strong>not</strong> depend on any other domain facade. All other
 * domain facades may depend on {@code iam}.
 *
 * <p>Current state: only the {@code User} entity and its repository exist, backing
 * the Review Session MVP endpoints. Authentication, sessions, credential hashing and
 * the public {@code iam} facade are not implemented yet.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.iam;
