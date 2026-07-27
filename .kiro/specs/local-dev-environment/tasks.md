# Implementation Plan: Local Development Environment

## Overview

Convert the feature design into a series of prompts for a code-generation LLM that will implement each step with incremental progress. Make sure that each prompt builds on the previous prompts, and ends with wiring things together. There should be no hanging or orphaned code that isn't integrated into a previous step. Focus ONLY on tasks that involve writing, modifying, or testing code.

Scope for this iteration: **Servicio_Backend (Spring Boot) + Servicio_Postgres**, orchestrated together on the `reviews-net` Docker network, with the API validated from the host through Postman (or any other `Cliente_HTTP_Local`: `curl`, HTTPie, IntelliJ HTTP Client, Bruno, Insomnia). The frontend service is explicitly out of scope and deferred to a future spec (see Requirement 18.4 and ADR-0004).

Implementation proceeds bottom-up: first the environment/secrets configuration and per-service artifacts (Dockerfile, init script, backend dependencies), then the Docker Compose file that wires them together, then the cross-platform operational scripts and `Makefile`, then the Postman starter collection, and finally the onboarding documentation and ADRs. Property tests derived from the design's Correctness Properties are placed as optional sub-tasks close to the code they validate.

All file paths in this plan are relative to the repository root `c:\devdocs\Kiro-hackathon\kiro-app-reviews\`.

## Tasks

- [x] 1. Bootstrap environment configuration and secret hygiene
  - [x] 1.1 Create the versioned `.env` template
    - Create `infra/docker/.env.example` with all variables listed in Requirement 8.3: `COMPOSE_PROJECT_NAME`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST_PORT`, `BACKEND_HOST_PORT`, `BACKEND_DEBUG_PORT`, `SPRING_PROFILES_ACTIVE`, `BACKEND_JAVA_OPTS`.
    - Populate defaults exactly as documented in the design: `reviews`, `5432`, `8080`, `5005`, `dev`, `-Xmx1024m`.
    - Set `POSTGRES_PASSWORD=CHANGE_ME` as an explicit placeholder — never a real credential.
    - Include header comment explaining the copy-and-edit workflow.
    - Do not include any variable related to a frontend service; that surface is deferred to a future spec.
    - _Requirements: 8.1, 8.3, 8.6, 9.2_

  - [x] 1.2 Update repository `.gitignore` to exclude the developer's `.env`
    - Append `.env` and `infra/docker/.env` entries to the root `.gitignore`.
    - Verify no existing `.env` file is currently tracked by git; if one exists, do not delete it in this task, only ensure it is ignored going forward.
    - _Requirements: 9.1_

  - [x] 1.3 Create `infra/docker/.dockerignore` to exclude host artifacts from build contexts
    - Add exclusions for `**/.git`, `**/target`, `**/.env`, `**/.idea`, `**/.vscode`.
    - _Requirements: 11.3_

- [x] 2. Prepare the PostgreSQL service artifacts
  - [x] 2.1 Create the PostgreSQL initialization script
    - Create `infra/docker/postgres/init/01-init-extensions.sql`.
    - Emit `CREATE EXTENSION IF NOT EXISTS` for `uuid-ossp`, `pg_trgm`, and `pgcrypto`.
    - _Requirements: 12.1, 12.2, 12.3_

- [x] 3. Prepare the Spring Boot backend service artifacts
  - [x] 3.1 Add the Spring dependencies required for the development environment to `backend/pom.xml`
    - Add `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, `spring-boot-devtools` (with `<optional>true</optional>` and `<scope>runtime</scope>` as needed to keep hot reload active in the dev image), `flyway-core`, `flyway-database-postgresql`, and the `org.postgresql:postgresql` JDBC driver.
    - Do not upgrade unrelated versions.
    - _Requirements: 4.2, 4.4, 7.2, 12.4, 12.5, 14.1_

  - [x] 3.2 Create the backend development Dockerfile
    - Create `infra/docker/backend/Dockerfile.dev` using base image `eclipse-temurin:21-jdk-alpine` (pinned, no `latest`).
    - Install `bash`, `wget`, and `git` (`wget` is required by the compose healthcheck against `/actuator/health`).
    - Set `WORKDIR /app`, copy `.mvn/`, `mvnw`, and `pom.xml`, then run `./mvnw dependency:go-offline -B || true` to pre-warm the Maven cache.
    - `EXPOSE 8080 5005`.
    - Set the command to `./mvnw spring-boot:run -Dspring-boot.run.fork=false` so DevTools observes the same JVM.
    - _Requirements: 4.1, 4.2, 5.1, 11.2, 11.3, 13.1_

  - [x] 3.3 Configure the Spring Boot `dev` profile
    - Update `backend/src/main/resources/application.yaml` to declare a `dev` profile with datasource properties driven by `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` (all injected by the compose file), Flyway enabled with the default `db/migration` classpath location, Actuator exposing at minimum the `health` endpoint on the default context path, and DevTools polling enabled so file-change events from the bind mount are picked up reliably on Windows/macOS.
    - Ensure the Actuator `health` endpoint responds in JSON with the standard Spring Boot payload (`status: UP` when healthy), so Postman assertions on `$.status == "UP"` are meaningful.
    - Do not commit any literal credential values.
    - _Requirements: 4.2, 4.4, 7.2, 12.4, 14.1, 14.2, 14.3, 15.3_

- [ ] 4. Wire the two services together in Docker Compose
  - [x] 4.1 Create `infra/docker/docker-compose.yml` with the full orchestration
    - Declare the `reviews-net` bridge network attached to both services (Requirement 6.1).
    - Declare named volumes `postgres-data` and `backend-m2-cache` with explicit `name:` prefixes matching the design.
    - Define the `postgres` service using `postgres:16-alpine`, mapping `${POSTGRES_HOST_PORT:-5432}` to `5432`, mounting `postgres-data` at `/var/lib/postgresql/data` and `./postgres/init` at `/docker-entrypoint-initdb.d:ro`, declaring the `pg_isready` healthcheck exactly as in the design, and using the `${POSTGRES_PASSWORD:?...}` required-variable syntax so compose fails fast when the variable is missing.
    - Define the `backend` service building from `infra/docker/backend/Dockerfile.dev`, publishing `${BACKEND_HOST_PORT:-8080}:8080` and `${BACKEND_DEBUG_PORT:-5005}:5005`, injecting `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JAVA_TOOL_OPTIONS` with the JDWP agent in `suspend=n` mode plus `${BACKEND_JAVA_OPTS}`, bind-mounting `../../backend` at `/app`, mounting the `backend-m2-cache` volume at `/root/.m2`, declaring the `/actuator/health` healthcheck, and setting `depends_on.postgres.condition: service_healthy`.
    - Set `restart: unless-stopped` on both services and `container_name` values `reviews-postgres`, `reviews-backend`.
    - Set the top-level `name: ${COMPOSE_PROJECT_NAME:-reviews}`.
    - Do NOT declare a frontend service. Leave a top-of-file comment noting that frontend is deferred to a future spec and that `reviews-net` is designed to accept new services without renaming (Requirement 6.4).
    - Ensure no literal credentials appear anywhere in the file.
    - _Requirements: 1.1, 1.3, 2.1, 2.4, 3.1, 4.1, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4, 7.5, 8.2, 8.3, 8.4, 9.3, 11.1, 11.2, 11.3, 12.1, 12.4, 13.1, 14.1, 15.1, 15.2_

  - [x] 4.2* Write property test for dependency ordering
    - **Property: backend starts only after postgres is healthy.**
    - **Validates: Requirements 7.3, 7.4**
    - Create `infra/docker/tests/property-dependency-ordering.sh` (bash) that runs `docker compose up -d`, polls `docker inspect` on both services, and asserts that at every observed timestamp until `postgres` is `healthy`, `backend` is either absent or in `Created` state (never `running`).
    - Parametrize the poll interval and the number of samples so the test can be run repeatedly across multiple `up`/`down` cycles.
    - _Requirements: 7.3, 7.4_

  - [ ] 4.3* Write property test for service isolation
    - **Property: A failure in one service does not take down the other.**
    - **Validates: Requirements 2.2, 2.3, 2.4**
    - Create `infra/docker/tests/property-service-isolation.sh` that iterates over each of the two services, kills that container with `docker kill`, waits `N` seconds, and asserts the other remains `running` (via `docker inspect --format '{{.State.Status}}'`).
    - _Requirements: 2.2, 2.3, 2.4_

  - [x] 4.4* Write property test for data persistence across restarts
    - **Property: Data written to postgres survives `docker compose down` / `up` cycles.**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Create `infra/docker/tests/property-data-persistence.sh` that, given a fresh environment, inserts a marker row into a temporary table via `docker compose exec postgres psql`, runs `docker compose down` (without `-v`), runs `docker compose up -d`, waits for the postgres healthcheck, and queries the marker row back — asserting it survived.
    - Parametrize the number of restart cycles so the property is checked over `k` iterations, not just one.
    - _Requirements: 3.1, 3.2, 3.3_

- [ ] 5. Checkpoint — backend + postgres boot from a single command and respond to Postman
  - Manually run the `dev-up` script (Task 6.1) once implemented, wait for both services to report healthy, and hit `http://localhost:${BACKEND_HOST_PORT}/actuator/health` from a `Cliente_HTTP_Local` (Postman or `curl`). Expected response: HTTP 200 with body `{"status":"UP"}`.
  - Ensure all non-optional tests pass. Ask the user if questions arise.

- [ ] 6. Implement cross-platform operational scripts
  - [x] 6.1 Create the `dev-up` scripts
    - Create `infra/docker/scripts/dev-up.sh` (POSIX bash) and `infra/docker/scripts/dev-up.cmd` (Windows batch).
    - Both scripts must resolve the repository root, verify the `.env` file exists at `<repo-root>/.env`; if missing, copy `infra/docker/.env.example` to `<repo-root>/.env`, print an instruction to edit `POSTGRES_PASSWORD`, and exit with code 1 without invoking `docker compose`.
    - When `.env` exists, invoke `docker compose --env-file <repo-root>/.env up -d "$@"` and then `docker compose ps` so the developer sees final status.
    - The `.sh` variant must set `set -euo pipefail` and be marked executable (`chmod +x`).
    - Both variants must produce the same observable effect on services and volumes.
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 8.4, 10.1, 10.2, 10.3, 16.1, 16.3_

  - [x] 6.2 Create the `dev-down` scripts
    - Create `infra/docker/scripts/dev-down.sh` and `.cmd`.
    - Both scripts invoke `docker compose --env-file <repo-root>/.env down` (without `-v`) so named volumes are preserved.
    - _Requirements: 3.2, 10.1, 10.2_

  - [x] 6.3 Create the `dev-reset` scripts
    - Create `infra/docker/scripts/dev-reset.sh` and `.cmd`.
    - Both scripts must prompt the developer for explicit confirmation (require typing `RESET` in bash via `read`, `set /p CONFIRM=` in cmd) before invoking `docker compose --env-file <repo-root>/.env down -v --remove-orphans`.
    - On non-confirmation, exit with code 1 and print a cancellation message.
    - _Requirements: 3.4, 3.5, 10.1, 10.2, 10.4, 13.3_

  - [x] 6.4 Create the `dev-logs` scripts
    - Create `infra/docker/scripts/dev-logs.sh` and `.cmd`.
    - When invoked with no arguments, run `docker compose logs -f` (aggregate follow of both services).
    - When invoked with a service name argument, forward it to `docker compose logs -f <service>`.
    - _Requirements: 10.1, 10.2, 10.5_

  - [x] 6.5 Create the root `Makefile`
    - Create `Makefile` at the repository root with `.PHONY` targets `up`, `down`, `reset`, `logs`, `backend-shell`, `db-shell`, `ps`.
    - Each target delegates to the appropriate `docker compose --env-file .env` invocation from `infra/docker/`.
    - `logs` must accept a `SERVICE=` variable to filter to one service.
    - _Requirements: 10.6_

  - [x] 6.6* Write property test for cross-platform script equivalence
    - **Property: `.sh` and `.cmd` variants of each operational command produce the same observable effect on services and volumes.**
    - **Validates: Requirements 10.1, 10.2**
    - Create `infra/docker/tests/property-script-equivalence.sh` that captures the state (list of running containers and named volumes) before and after invoking each `.sh` script, and asserts the expected pre/post state transitions match the design's contract for each command.
    - The `.cmd` variants cannot execute on Unix runners, so the test must document the expected equivalence and assert the `.sh` variant behaves per contract; developers on Windows run the equivalent assertions manually.
    - _Requirements: 10.1, 10.2_

- [x] 7. Provide a starter Postman collection for backend validation
  - [x] 7.1 Create the versioned Postman collection
    - Create `docs/postman/reviews-local.postman_collection.json` in Postman Collection v2.1 format.
    - Include, at minimum, one request `GET {{base_url}}/actuator/health` with a test assertion that verifies HTTP 200 and JSON field `$.status == "UP"`.
    - Group requests under a folder named `Smoke` to make room for future domain requests without restructuring.
    - _Requirements: 15.3, 15.4_

  - [x] 7.2 Create the matching Postman environment file
    - Create `docs/postman/reviews-local.postman_environment.json` with a variable `base_url` defaulting to `http://localhost:8080` and a variable `backend_host_port` defaulting to `8080`.
    - Document in the file header comment that `base_url` should be rebuilt as `http://localhost:{{backend_host_port}}` when the developer overrides `BACKEND_HOST_PORT` in `.env`.
    - _Requirements: 15.4, 15.5_

- [ ] 8. Write onboarding documentation and ADRs
  - [x] 8.1 Write the onboarding guide
    - Create `docs/local-dev-environment.md`.
    - Document the host prerequisites: Docker Engine ≥ 24.0, Docker Compose V2, optionally `make`, an IDE with JDWP support, and a `Cliente_HTTP_Local` (Postman recommended; Bruno, Insomnia, HTTPie, `curl` and IntelliJ HTTP Client also supported).
    - Document the four operational commands (`dev-up`, `dev-down`, `dev-reset`, `dev-logs`) in both `.sh` and `.cmd` forms, plus the `make` equivalents.
    - Document the `.env` bootstrap procedure (copy from `.env.example`, set `POSTGRES_PASSWORD`).
    - Document default URLs and published ports: backend at `http://localhost:8080`, backend health at `http://localhost:8080/actuator/health`, postgres JDBC at `jdbc:postgresql://localhost:5432/reviews`, JDWP at `localhost:5005`.
    - Include a `Probando la API con Postman` section that documents importing `docs/postman/reviews-local.postman_collection.json` together with `docs/postman/reviews-local.postman_environment.json`, selecting the environment, and running the `Smoke > GET /actuator/health` request as a smoke test. Show the same request as `curl` and HTTPie one-liners for developers who do not use Postman.
    - Document the known error scenarios: port already in use, `POSTGRES_PASSWORD` not set, Docker Engine not running, `pom.xml` changes not picked up by DevTools, Flyway migration failure.
    - Note explicitly that the frontend service is out of scope for this iteration and will be added in a future spec.
    - _Requirements: 4.5, 15.5, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_

  - [x] 8.2 Write ADR-0001: PostgreSQL as the database of the environment
    - Create `docs/adr/0001-choose-postgresql.md` with sections Context, Decision, Alternatives Considered (MySQL 8, MariaDB, H2), Consequences, aligned with the comparison table in the design's Component 1.
    - _Requirements: 18.1, 18.5_

  - [x] 8.3 Write ADR-0002: Docker Compose as the local orchestrator
    - Create `docs/adr/0002-choose-docker-compose.md` with sections Context, Decision, Alternatives Considered (Podman Compose, Tilt, Skaffold, Nix, pure Devcontainers), Consequences.
    - _Requirements: 18.2, 18.5_

  - [ ] 8.4 Write ADR-0003: Bind mount for backend source plus named volume for Maven cache
    - Create `docs/adr/0003-bind-mount-and-m2-cache.md` with sections Context, Decision, Alternatives Considered (full COPY into image with rebuilds, `docker sync`, host-only development without containers), Consequences — documenting the split between source-code bind mount and derived-artifact named volume for `~/.m2`.
    - _Requirements: 18.3, 18.5_

  - [ ] 8.5 Write ADR-0004: Defer the frontend service; validate backend with Postman
    - Create `docs/adr/0004-defer-frontend-postman-first.md` with sections Context, Decision, Alternatives Considered (bundle Next.js dev server now, use Storybook-only, mock the API), Consequences — including the explicit expectation that a follow-up spec will extend `reviews-net` and the Archivo_Env to add a frontend service without renaming existing resources.
    - _Requirements: 18.4, 18.5_

- [ ]* 9. Implement infrastructure verification tests (optional suite)
  - [ ]* 9.1 Write property test for bootstrap reproducibility
    - **Property: two clean environments from the same commit and `.env` produce the same running services on the same ports with pinned image references.**
    - **Validates: Requirements 1.1, 1.2, 11.1, 11.2, 11.3**
    - Create `infra/docker/tests/property-reproducibility.sh` that runs `docker compose config` and asserts the resolved image references match the pinned tags (`postgres:16-alpine`, `eclipse-temurin:21-jdk-alpine`), that no `image: *:latest` appears anywhere, and that a second `docker compose up -d` from a clean state produces the same `docker compose ps` output as the first.
    - _Requirements: 1.1, 1.2, 11.1, 11.2, 11.3_

  - [ ]* 9.2 Write property test for arranque idempotency
    - **Property: repeated `docker compose up -d` invocations converge to the same state without duplicating containers, volumes, or networks.**
    - **Validates: Requirements 1.5**
    - Create `infra/docker/tests/property-idempotency.sh` that runs `docker compose up -d` `k` times, then asserts (a) `docker ps` shows exactly one container per service, (b) `docker volume ls` shows exactly one volume per named volume, (c) `docker network ls` shows exactly one `reviews-net` network.
    - _Requirements: 1.5_

  - [ ]* 9.3 Write property test for secret hygiene
    - **Property: no tracked file contains a real credential; `.env` is git-ignored; `.env.example` uses non-exploitable placeholders; `docker-compose.yml` references credentials only through `${...}` interpolation.**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4**
    - Create `infra/docker/tests/property-no-secrets.sh` that runs `git ls-files` and asserts `.env` is not tracked, `git grep -n 'CHANGE_ME'` against `infra/docker/.env.example` returns a match (placeholder present), and `git grep -nE 'POSTGRES_PASSWORD\s*[:=]\s*[^$]' -- infra/docker/docker-compose.yml` returns no matches (compose never binds a literal password).
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ]* 9.4 Write property test for port conflict detection and reset behavior
    - Create `infra/docker/tests/test-reset-and-ports.sh` covering: (a) after `dev-reset`, `docker volume ls` no longer contains `reviews-postgres-data` nor `reviews-backend-m2-cache`; (b) if a port is manually occupied (bind a listener on `${BACKEND_HOST_PORT}`) and `dev-up` is invoked, the affected service fails but the other still starts.
    - _Requirements: 3.4, 3.5, 13.3, 16.1, 16.2, 16.3_

  - [ ]* 9.5 Write API reachability smoke test from a Cliente_HTTP_Local
    - **Property: the backend API is reachable from the host through `http://localhost:${BACKEND_HOST_PORT}` without any proxy configuration, and `/actuator/health` returns HTTP 200 with `status: UP`.**
    - **Validates: Requirements 14.2, 15.1, 15.2, 15.3**
    - Create `infra/docker/tests/property-api-reachability.sh` that runs `dev-up`, waits for the backend healthcheck, and then invokes `curl -fsS http://localhost:${BACKEND_HOST_PORT:-8080}/actuator/health` from the host, asserting HTTP 200 and JSON field `$.status == "UP"`.
    - Optionally invoke the Postman collection through `newman` (Postman CLI) as a second, equivalent check, if `newman` is available on the host.
    - _Requirements: 14.2, 15.1, 15.2, 15.3_

- [ ] 10. Final checkpoint — full environment, Postman collection, docs, and tests
  - Ensure all non-optional tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for a faster MVP. Core implementation tasks (all non-starred sub-tasks) must be completed.
- Each task references specific granular requirements (e.g., 7.3 not just "Req 7") for traceability against the requirements document.
- Property tests are placed close to the code they validate: dependency ordering, isolation, and persistence next to the compose file (Task 4); cross-platform equivalence next to the scripts (Task 6); reproducibility, idempotency, secret hygiene, port conflict, and API reachability grouped at the end (Task 9) since they exercise the whole system.
- The Postman starter collection (Task 7) is the primary manual validation surface for this iteration, replacing the browser-based validation that a frontend would have provided.
- Checkpoints (Task 5 and Task 10) exist to pause and validate incremental progress before moving on.
- No task involves user acceptance testing, deployment, or manual end-to-end runs; automated validation lives in `infra/docker/tests/`, and manual validation is documented in `docs/local-dev-environment.md`.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "2.1", "3.1", "3.3"] },
    { "id": 1, "tasks": ["3.2"] },
    { "id": 2, "tasks": ["4.1"] },
    { "id": 3, "tasks": ["4.2", "4.3", "4.4", "6.1", "6.2", "6.3", "6.4", "6.5", "7.1", "7.2", "8.1", "8.2", "8.3", "8.4", "8.5"] },
    { "id": 4, "tasks": ["6.6", "9.1", "9.2", "9.3", "9.4", "9.5"] }
  ]
}
```
