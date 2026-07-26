/**
 * Domain package {@code wspr} (Workspaces, Projects and Invitations).
 *
 * <p>Responsibility: manages Workspaces, Projects and Invitations. It is the sole
 * domain that knows the {@code owner(Project) → ProjectAdmin} ownership relation and
 * the {@code invitations(Project) → Client} affiliation relation. Owns the unified
 * authorization function {@code canAccessProject} (dual key: role + relation).
 *
 * <p>Role in the facade DAG:
 * <ul>
 *   <li>Depends on: {@code iam} (to resolve {@code UserRole} of the principal).</li>
 *   <li>Depended upon by: {@code rs}, {@code notif}, {@code boff}.</li>
 * </ul>
 *
 * <p>Current state: {@code Workspace} and {@code Project} entities plus their
 * repositories exist; ownership is exposed via {@code Project#isOwnedBy}. Invitations,
 * {@code canAccessProject} and the public {@code wspr} facade are not implemented yet —
 * {@code rs} currently checks ownership directly as an MVP stand-in.
 *
 * <p>Cross-cutting dependencies allowed: {@code shared}.
 *
 * <p>See {@code design.md} → sections <em>Components and Interfaces</em> and
 * <em>Vista de Dominios y Fachadas</em>.
 */
package io.github.bsidedevs.api_review.wspr;
