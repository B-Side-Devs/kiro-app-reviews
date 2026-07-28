# Implementation Plan: Review Session Core API

## Overview

This implementation plan covers the review session core API feature, which provides a complete backend for managing review session lifecycle. The feature implements:

- A facade pattern (`rs.ReviewSessionFacade`) for domain access
- Complete state machine with `TransitionMatrix` for state transitions
- Authorization via double-key access (user role + project relationship)
- Result<T, DomainError> pattern instead of exceptions
- Problem+JSON error format with correlation ID headers
- Optimistic locking for concurrency

The implementation follows the design document structure: domain layer first, then repository updates, then REST layer, and finally testing and configuration.

## Tasks

- [x] 1. Update domain layer - TransitionMatrix and Facade interfaces
  - [x] 1.1 Create `TransitionMatrix` enum and evaluation logic
    - Implement command enum: CREATE, UPDATE_METADATA, START, COMPLETE, REOPEN, ARCHIVE, DELETE
    - Implement verdict record: allowed, targetState, code
    - Implement `evaluate(state, command, persistenceStatus)` method
    - Handle DELETED absorbing state
    - Validate UPDATE_METADATA only from DRAFT
    - Validate COMPLETE requires RECORDING and OK persistence
    - Validate REOPEN/ARCHIVE/DELETE require OWNER reason
    - _Requirements: 2.1, 2.2, 2.7-2.13, 3.1-3.3, 4.2-4.3, 5.9, 5.10_

  - [x] 1.2 Create `ReviewSessionFacade` interface
    - Define ReviewSessionView record (14 fields)
    - Define NewSessionData record (metadata fields)
    - Define MetadataPatch with FieldPatch<T> types
    - Define ListingScope enum: ACTIVE, ALL
    - Declare all 10 public methods: get, list, currentState, create, updateMetadata, startRecording, completeRecording, reopen, archive, delete
    - All methods return Result<View, DomainError>
    - _Requirements: 1.1, 1.2, 1.3, 1.10_

- [x] 2. Implement domain layer - Facade service and validation
  - [x] 2.1 Implement `ReviewSessionService` (facade implementation)
    - Implement common execution algorithm with transaction wrapping
    - Resolve principal before database access
    - Call WorkspaceProjectFacade.canAccessProject first
    - Handle not found vs not authorized indistinguishably
    - Validate metadata for CREATE/UPDATE_METADATA
    - Evaluate TransitionMatrix before persistence
    - Apply mutations (package-private methods)
    - Handle OptimisticLockingFailure with INVALID_TRANSITION
    - Catch all exceptions and return INTERNAL
    - _Requirements: 1.4, 1.8, 1.9, 2.14, 2.16, 3.7-3.8, 4.8, 5.1, 5.7-5.8, 5.14, 9.10_

  - [x] 2.2 Implement `MetadataValidator`
    - Validate name: absent or 1-200 chars after trim
    - Validate description: absent or 1-2000 chars
    - Validate targetUrl: absent or http(s) URL ≤2048 chars
    - Return FieldError list for validation failures
    - _Requirements: 9.1, 9.2, 9.4-9.6_

- [x] 3. Update repository and data models
  - [x] 3.1 Update `ReviewSession` entity
    - Add name, description, targetUrl fields
    - Add version field for optimistic locking (@Version)
    - Convert timestamps to TIMESTAMPTZ(3) via AttributeConverter
    - Change state persistence to package-private methods (applyStart, applyComplete, etc.)
    - Remove state validation from public methods
    - _Requirements: 2.15, 2.16, 11.1, 11.4_

  - [x] 3.2 Create `MetadataPatch` domain type
    - Implement FieldPatch<T> sealed interface (Unchanged, Clear, Set)
    - Implement MetadataPatch with name, description, targetUrl fields
    - Implement applyPatch method
    - _Requirements: 9.7-9.9_

- [x] 4. Create migration files for schema changes
  - [x] 4.1 Create `V5__Extend_review_sessions.sql`
    - Add name (VARCHAR(200)), description (VARCHAR(2000)), target_url (VARCHAR(2048)) columns
    - Add version (BIGINT NOT NULL DEFAULT 0) for optimistic locking
    - Add CHECK constraints for state and persistence_status
    - Convert all timestamp columns to TIMESTAMPTZ(3)
    - Create index idx_review_sessions_project_listing
    - Create trigger for immutable project_id
    - _Requirements: 11.1, 11.3, 11.5, 11.8_

  - [x] 4.2 Create `V6__Create_invitations_and_project_block.sql`
    - Add blocked column to projects table (BOOLEAN NOT NULL DEFAULT FALSE)
    - Create invitations table with composite unique constraint
    - Add foreign keys to projects and users
    - Add CHECK constraint for invitation status
    - Create index idx_invitations_project_invitee
    - _Requirements: 11.7, 11.15, 11.16_

  - [x] 4.3 Create `V7__Seed_authorization_actors.sql`
    - Insert CLIENT user with fixed UUID
    - Insert PROJECT_ADMIN user (non-owner) with fixed UUID
    - Insert PLATFORM_ADMIN user with fixed UUID
    - Insert ACCEPTED invitation from CLIENT to seed project
    - Use ON CONFLICT DO NOTHING for idempotency
    - _Requirements: 11.9-11.10, 11.16_

- [x] 5. Implement WorkspaceProjectFacade for authorization
  - [x] 5.1 Create `WorkspaceProjectFacade` interface
    - Declare canAccessProject method
    - Return AccessDecision record (granted, reason)
    - _Requirements: 1.5, 5.1_

  - [x] 5.2 Implement `ProjectAccessService` and `AccessDecisionEvaluator`
    - Implement evaluateDecision method
    - Handle PROJECT_ADMIN owner check
    - Handle CLIENT with invitation lookup
    - Check project blocked status
    - Implement timeout with 2000ms budget
    - Handle all error paths as DENIED_UNKNOWN
    - _Requirements: 5.2-5.6, 5.11-5.13_

  - [x] 5.3 Update `Project` entity
    - Add blocked field
    - _Requirement: 5.13_

- [x] 6. Implement Principal provider and correlation filtering
  - [x] 6.1 Create `PrincipalProvider` interface and `AuthenticatedPrincipal` record
    - Define resolve method with header values and correlationId
    - Define AuthenticatedPrincipal record (userId, role, correlationId)
    - _Requirements: 6.2, 6.7_

  - [x] 6.2 Implement `HeaderPrincipalProvider`
    - Validate exactly one header value
    - Validate header length ≤64
    - Parse UserId with type-safe parsing
    - Look up user from UserRepository
    - Return UNAUTHENTICATED for any failure
    - _Requirements: 6.3-6.4, 6.9-6.10_

  - [x] 6.3 Create `CorrelationIdFilter`
    - Implement OncePerRequestFilter
    - Extract or generate correlationId
    - Set in response header (exactly one, using setHeader not addHeader)
    - Set in MDC via CorrelationContext
    - Apply to all paths; IdentityFilter applies only to /api/v1/*
    - _Requirements: 10.12-10.18_

- [x] 7. Implement REST layer - Controller
  - [x] 7.1 Update `ReviewSessionController`
    - Expose POST /api/v1/projects/{projectId}/review-sessions for create (201 + Location header)
    - Expose GET /api/v1/review-sessions/{sessionId} for get (200)
    - Expose GET /api/v1/projects/{projectId}/review-sessions?scope= for list (200 array)
    - Expose PATCH /api/v1/review-sessions/{sessionId} for updateMetadata (200)
    - Expose POST /api/v1/review-sessions/{sessionId}/start, /complete, /reopen, /archive (200)
    - Expose DELETE /api/v1/review-sessions/{sessionId} for delete (204 no body)
    - Parse path variables with type-safe parsing (ProjectId, ReviewSessionId)
    - Add scope parameter for list endpoint (active, all); default to active when absent
    - Reject invalid scope with 400 Problem+JSON
    - Transition POST endpoints declare no @RequestBody; ignore any received body
    - _Requirements: 7.1-7.9, 7.13, 8.1-8.4, 8.6, 8.10_

  - [x] 7.2 Create `IdentityFilter` and `PrincipalArgumentResolver`
    - Implement IdentityFilter (OncePerRequestFilter) registered only for /api/v1/* pattern
    - Resolve principal via PrincipalProvider; on failure write 401 Problem+JSON with ObjectMapper
    - Implement PrincipalArgumentResolver to read resolved principal from request attribute
    - Filter order: CorrelationIdFilter (0) → IdentityFilter (1)
    - _Requirements: 6.1, 6.5-6.6, 10.11_

  - [x] 7.3 Update `ReviewSessionDto`
    - Create ReviewSessionResponse record with 14 fields matching ReviewSessionView order
    - Serialize Instants as ISO 8601 with millisecond precision and UTC offset (JacksonConfig)
    - Flatten Optional to null (Jackson ALWAYS inclusion so null fields are explicit)
    - Add persistenceStatus field
    - _Requirements: 1.6, 4.6, 7.10-7.12_

- [x] 8. Implement REST layer - Exception handling
  - [x] 8.1 Create `ProblemFactory`
    - Map DomainError category to HTTP status per Modelo 4
    - Build ProblemDetail with type URI, title, detail, correlationId
    - Include code and currentState for INVALID_TRANSITION errors
    - Include errors list for VALIDATION errors; omit otherwise
    - _Requirements: 10.1-10.4, 10.9_

  - [x] 8.2 Create `RestExceptionHandler` (@RestControllerAdvice)
    - Handle DomainError (all six categories → HTTP 400/401/403/404/409/500)
    - Handle MethodArgumentNotValidException → 400 with field errors
    - Handle HttpMessageNotReadableException → 400 with generic detail, no errors field
    - Handle MethodArgumentTypeMismatchException (invalid UUID segment or scope) → 400 enumerating the parameter
    - Catch any unhandled Exception → 500 with generic detail, no stack trace
    - Return Content-Type: application/problem+json for all error responses
    - _Requirements: 10.2, 10.3, 10.5-10.10, 10.15-10.16_

  - [x] 8.3 Wire IdentityFilter error responses through ProblemFactory
    - Reuse ProblemFactory for 401 responses written directly from IdentityFilter
    - Write body with ObjectMapper, Content-Type: application/problem+json
    - _Requirements: 6.3, 10.11_

- [x] 9. Implement security audit logging
  - [x] 9.1 Create `SecurityAuditLogger`
    - Implement denied(userId, resourceId, reason, correlationId) method
    - Implement unauthenticated(rawHeaderPresent, correlationId) method
    - Use dedicated "security" logger separate from operational log
    - Catch and suppress all internal exceptions so callers are never affected
    - _Requirements: 5.8, 5.14, 6.11_

- [x] 10. Wire integration - WorkspaceProjectFacade and ReviewSessionRepository
  - [x] 10.1 Wire WorkspaceProjectFacade into ReviewSessionService
    - Inject WorkspaceProjectFacade bean
    - Call canAccessProject before state evaluation in every operation
    - Log security violations via SecurityAuditLogger
    - Translate AccessDecision denial to NOT_FOUND; OWNER-only commands to UNAUTHORIZED
    - _Requirements: 1.5, 3.9, 5.1, 5.7-5.8, 5.9-5.10_

  - [x] 10.2 Update ReviewSessionRepository
    - Add findForListing(projectId, excludedStates, limit) method
    - ORDER BY created_at DESC, review_session_id ASC (covered by idx_review_sessions_project_listing)
    - Apply limit of 200 rows at query level
    - _Requirements: 8.4-8.5, 8.11_

- [x] 11. Update Entorno_Local configuration
  - [x] 11.1 Verify /actuator/health is reachable without X-User-Id header (IdentityFilter excludes /actuator/*)
    - _Requirements: 12.2, 12.3_

- [x] 12. Update Postman collection
  - [-] 12.1 Update `docs/postman/reviews-mvp.postman_collection.json`
    - Restructure into three folders: `Smoke`, `Review Sessions`, `Review Sessions/Rejections`
    - **Smoke**: keep existing `GET /actuator/health` request with HTTP 200 test
    - **Review Sessions**: add 9 requests covering the full lifecycle in order:
      1. `POST /api/v1/projects/{{seed_project_id}}/review-sessions` (create → 201; save `reviewSessionId` to `current_review_session_id`)
      2. `GET /api/v1/review-sessions/{{current_review_session_id}}` (get → 200)
      3. `GET /api/v1/projects/{{seed_project_id}}/review-sessions` (list active → 200 array)
      4. `PATCH /api/v1/review-sessions/{{current_review_session_id}}` (updateMetadata → 200)
      5. `POST /api/v1/review-sessions/{{current_review_session_id}}/start` (start → 200, state=RECORDING)
      6. `POST /api/v1/review-sessions/{{current_review_session_id}}/complete` (complete → 200, state=COMPLETED)
      7. `POST /api/v1/review-sessions/{{current_review_session_id}}/reopen` (reopen → 200, state=REOPENED)
      8. `POST /api/v1/review-sessions/{{current_review_session_id}}/archive` (archive → 200 via complete+archive path — adjust if needed)
      9. `DELETE /api/v1/review-sessions/{{current_review_session_id}}` (delete → 204)
    - **Review Sessions/Rejections**: add 3 rejection requests:
      1. Transition conflict: attempt `start` on a non-DRAFT session → expect 409 with `currentState` and `application/problem+json`
      2. Non-owner admin: attempt `delete` with `seed_other_admin_id` → expect 404 and `application/problem+json`
      3. Missing identity header: attempt `GET` without `X-User-Id` → expect 401 with `correlationId` populated
    - Add shared pre-request script that calls `postman.setNextRequest(null)` when `current_review_session_id` is empty
    - Each lifecycle request verifies expected HTTP status code and `state` field in response
    - Each rejection request verifies `Content-Type: application/problem+json` and `correlationId` present
    - _Requirements: 13.1-13.9_

  - [x] 12.2 Create Postman environment file `docs/postman/reviews-local.postman_environment.json`
    - Include exactly 7 variables with populated initial values:
      - `base_url`: `http://localhost:{{backend_host_port}}`
      - `backend_host_port`: default port value from docker-compose
      - `seed_project_id`: `018e0c5c-2b3c-7000-8000-000000000001`
      - `seed_owner_admin_id`: `018e0c5a-7b3f-7000-8000-000000000001`
      - `seed_client_id`: `018e0c5a-7b3f-7000-8000-000000000002`
      - `seed_other_admin_id`: `018e0c5a-7b3f-7000-8000-000000000003`
      - `current_review_session_id`: (empty initial value)
    - _Requirements: 13.4, 13.7, 13.12_

- [ ] 13. Write automated tests

  - [ ] 13.1 Checkpoint - Ensure backend compiles and all existing tests pass before adding new ones
    - Run `./mvnw test -pl backend` and confirm green build
    - Ask the user if any issues arise before continuing

  - [ ] 13.2 Write TransitionMatrix property-based tests (jqwik)
    - [ ]* 13.2.1 Write property test for state transition validity (Property 1)
      - **Property 1: State Transition Validity**
      - Use `@ForAll` over all `ReviewSessionState × Command × PersistenceStatus` combinations (42 cells)
      - Assert DELETED is absorbing: all commands return `allowed=false`
      - Assert UPDATE_METADATA only allowed from DRAFT; code=METADATA_NOT_EDITABLE otherwise
      - Assert COMPLETE requires RECORDING + OK; code=PERSISTENCE_PENDING when persistence≠OK
      - Assert REOPEN only from COMPLETED; ARCHIVE only from COMPLETED
      - Assert DELETE blocked from RECORDING and from DELETED
      - **Validates: Requirements 2.1, 2.2, 2.7-2.13, 3.2, 3.3, 4.2-4.3**

    - [ ]* 13.2.2 Write property test for DELETED absorbing state (Property 3)
      - **Property 3: DELETED State is Absorbing**
      - For all commands, `TransitionMatrix.evaluate(DELETED, command, anyStatus).allowed = false`
      - Assert code = "INVALID_TRANSITION" for every command
      - **Validates: Requirements 2.9, 3.3**

  - [ ] 13.3 Write ReviewSession entity unit tests
    - [ ]* 13.3.1 Write property test for state monotonicity (Property 2)
      - **Property 2: State Monotonicity**
      - Generate valid ReviewSession instances via arbitrary builders with injected Clock
      - Apply transitions in valid sequence; assert `completedAt ≥ startedRecordingAt`
      - Assert `reopenedAt ≥ completedAt` and `archivedAt ≥ completedAt` when present
      - Assert `deletedAt ≥ createdAt` and `deletedAt ≥` all other non-null timestamps
      - Assert all timestamps are truncated to milliseconds in UTC
      - **Validates: Requirements 2.15, 2.2**

    - [ ]* 13.3.2 Write unit tests for FieldPatch / MetadataPatch semantics (Property 9)
      - **Property 9: FieldPatch Semantics**
      - `Unchanged` patch: field value preserved unchanged
      - `Clear` patch: field value becomes null
      - `Set(value)` patch: field value = trim(value)
      - Apply multiple patches in sequence; assert only targeted fields change
      - **Validates: Requirements 9.8, 9.9**

  - [ ] 13.4 Write MetadataValidator property-based tests (jqwik)
    - [ ]* 13.4.1 Write property test for metadata validation coverage (Property 8)
      - **Property 8: Metadata Validation Coverage**
      - `name`: generate strings of length 0 (reject), 1-200 (accept), >200 (reject)
      - `description`: generate strings of length >2000 (reject), otherwise accept when non-empty
      - `targetUrl`: generate valid http/https URLs (accept), non-URL strings (reject), >2048 chars (reject)
      - Assert error list contains exactly the invalid field(s) per input combination
      - **Validates: Requirements 9.1, 9.4, 9.5, 9.6**

  - [ ] 13.5 Write ReviewSessionService / facade integration tests (Spring @SpringBootTest with Testcontainers)
    - [ ]* 13.5.1 Write integration test for owner-only operations (Property 5)
      - **Property 5: Owner-Only Operations**
      - Seed a project with an OWNER and a CLIENT with ACCEPTED invitation
      - Attempt REOPEN, ARCHIVE, DELETE with CLIENT principal; assert 403 UNAUTHORIZED / OWNER_ONLY
      - Attempt REOPEN, ARCHIVE, DELETE with non-owner PROJECT_ADMIN; assert 404 NOT_FOUND
      - **Validates: Requirements 3.9, 5.9, 5.10**

    - [ ]* 13.5.2 Write integration test for access decision consistency (Property 4)
      - **Property 4: Access Decision Consistency**
      - OWNER principal → decision.granted=true, reason=OWNER
      - CLIENT with ACCEPTED invitation, unblocked project → decision.granted=true, reason=ACCEPTED_INVITATION
      - CLIENT with ACCEPTED invitation, blocked project → denied, reason=DENIED_PROJECT_BLOCKED
      - CLIENT with PENDING invitation → denied, reason=DENIED_PENDING_INVITATION
      - PLATFORM_ADMIN → denied, reason=DENIED_NO_RELATION
      - Unknown projectId → denied, reason=DENIED_UNKNOWN
      - **Validates: Requirements 5.1, 5.2, 5.3, 5.5, 5.6, 5.11-5.13**

    - [ ]* 13.5.3 Write integration test for optimistic locking (Property 6)
      - **Property 6: Optimistic Locking Behavior**
      - Create a session in DRAFT; fire two concurrent startRecording calls
      - Assert exactly one succeeds (version incremented); other returns INVALID_TRANSITION / CONCURRENT_TRANSITION with the resultant state
      - **Validates: Requirement 2.16**

    - [ ]* 13.5.4 Write integration test for transaction atomicity (Property 7)
      - **Property 7: Transaction Atomicity**
      - Simulate persistence failure (mock repository save to throw); assert state unchanged and INTERNAL returned
      - Assert successful transitions atomically update state + timestamp + version in one transaction
      - **Validates: Requirements 2.14, 3.7-3.8, 4.8, 9.10**

  - [ ] 13.6 Write controller unit tests (MockMvc / WebMvcTest)
    - [ ]* 13.6.1 Write controller tests for error category completeness (Property 10 & 11)
      - **Property 10: Error Category Completeness** / **Property 11: Error Information Completeness**
      - For each endpoint, mock facade to return each of the 6 error categories
      - Assert correct HTTP status, Content-Type: application/problem+json, correlationId header and body field populated
      - Assert INVALID_TRANSITION responses include `code` and `currentState` fields
      - Assert VALIDATION responses include `errors` list; other categories omit it
      - Assert no 2xx status is used to carry an error
      - **Validates: Requirements 1.4, 10.1-10.9, 10.15**

    - [ ]* 13.6.2 Write controller tests for endpoint routing and response shape
      - POST create → 201 with Location header pointing to GET route
      - GET, PATCH, POST transitions → 200 with ReviewSessionResponse body
      - DELETE → 204 with empty body
      - All 14 fields present in response; null fields explicitly serialised as null
      - Invalid UUID segment → 400 Problem+JSON enumerating the segment
      - Invalid scope value → 400 Problem+JSON enumerating the `scope` parameter
      - Missing X-User-Id → 401 Problem+JSON with correlationId
      - _Requirements: 7.1-7.13, 8.1-8.6, 8.8, 8.10_

  - [ ] 13.7 Write ArchUnit architecture tests
    - [ ]* 13.7.1 Write ArchUnit tests for package boundary rules
      - No class in `rest` imports `rs.ReviewSession`, `rs.ReviewSessionRepository`, `rs.ReviewSessionService`, `wspr.Project`, `wspr.Invitation`, or their repositories
      - No class outside `rs` calls package-private mutators (applyStart, applyComplete, applyReopen, applyArchive, applyDelete, applyMetadata)
      - `rs` does not import `wspr.Project` or `wspr.InvitationRepository`; only `wspr.WorkspaceProjectFacade` and `shared.AccessDecision`
      - `wspr` does not import anything from `rs`
      - _Requirements: 1.6, 1.7, 1.10_

  - [ ] 13.8 Write property test for listing scope correctness (jqwik)
    - [ ]* 13.8.1 Write property test for listing scope (Property 13)
      - **Property 13: Listing Scope Correctness**
      - scope=ACTIVE (or absent): no ARCHIVED or DELETED sessions in result
      - scope=ALL: no DELETED sessions in result (ARCHIVED present)
      - Empty project returns 200 with empty array for both scopes
      - **Validates: Requirements 8.1, 8.2, 8.3, 8.6**

  - [ ] 13.9 Write property test for idempotent deletion (jqwik)
    - [ ]* 13.9.1 Write property test for idempotent deletion (Property 12)
      - **Property 12: Idempotent Deletion**
      - After successful delete: state=DELETED, deletedAt populated
      - Second delete attempt: INVALID_TRANSITION with deletedAt unchanged
      - **Validates: Requirements 3.3, 3.10**

  - [ ] 13.10 Final checkpoint - Ensure all tests pass
    - Run `./mvnw test -pl backend` and confirm green build
    - Verify ArchUnit tests report no violations
    - Ask the user if any questions arise before closing the feature

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property-based tests (jqwik) validate universal correctness properties; minimum 100 iterations per property
- Unit tests validate specific examples and edge cases
- Architecture tests (ArchUnit) validate structural package boundaries
- The model MUST NOT implement sub-tasks postfixed with `*`; those are property/unit test tasks to be run, not generated

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "5.1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "3.2", "6.1", "6.2"] },
    { "id": 2, "tasks": ["3.1", "4.1", "4.2", "4.3", "5.2", "5.3", "6.3"] },
    { "id": 3, "tasks": ["7.1", "7.2", "7.3", "9.1", "10.2"] },
    { "id": 4, "tasks": ["8.1", "8.2", "8.3", "10.1"] },
    { "id": 5, "tasks": ["11.1", "12.1", "12.2"] },
    { "id": 6, "tasks": ["13.1"] },
    { "id": 7, "tasks": ["13.2.1", "13.2.2", "13.3.1", "13.3.2", "13.4.1", "13.7.1"] },
    { "id": 8, "tasks": ["13.5.1", "13.5.2", "13.5.3", "13.5.4", "13.6.1", "13.6.2", "13.8.1", "13.9.1"] },
    { "id": 9, "tasks": ["13.10"] }
  ]
}
```
