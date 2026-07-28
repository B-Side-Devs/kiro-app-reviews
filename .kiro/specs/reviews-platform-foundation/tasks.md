# Implementation Plan: Reviews Platform Foundation

## Overview

Convert the feature design into a series of prompts for a code-generation LLM that will implement each step with incremental progress. Make sure that each prompt builds on the previous prompts, and ends with wiring things together. There should be no hanging or orphaned code that isn't integrated into a previous step. Focus ONLY on tasks that involve writing, modifying, or testing code.

Este plan implementa el **spec de fundación** de la plataforma Reviews. Su alcance es transversal y **no** desarrolla los detalles internos de cada paquete de dominio (endpoints concretos, DTOs completos, pantallas, migraciones específicas): esos detalles se abordarán en specs posteriores tomando este documento como marco.

### Estrategia por etapas

El plan se organiza en **dos etapas explícitas** con numeración secuencial continua. La prioridad es el **desarrollo progresivo y modular del core de dominio**: construir y ejercitar el núcleo módulo por módulo, cada uno con su fachada pública y sus tests de unidad/propiedades, sin bloquearse por infraestructura transversal pesada.

- **Etapa 1 — Core de dominio (tareas 1 a 18)**. Materializa el esqueleto del monolito, el paquete `shared`, la observabilidad mínima (correlación + logging estructurado), el framework de errores Problem+JSON y, en orden de dependencia del grafo de fachadas (`iam` → `wspr` → `rs` → `art` → `notif`/`boff`), la **fachada pública** y la **lógica de dominio** de cada paquete con sus property tests. Cierra con la capa REST base + OpenAPI invocando las fachadas, el paquete `test-fixtures`, las reglas ArchUnit y los ADRs de fundación. En esta etapa **no** se implementa hashing de credenciales, ni filtro HTTP de autenticación, ni Spring Security: donde el diseño exige un `AuthenticatedPrincipal`, éste se resuelve mediante un **proveedor de principal sustituible** (interfaz con implementación stub/fixture de test), cuyo reemplazo real llega en la Etapa 2.
- **Etapa 2 — Seguridad aplicada, end-to-end y frontend (tareas 19 a 27)**. Depende íntegramente de la Etapa 1. Incorpora hashing de credenciales, autenticación real con Spring Security, filtro global de autorización HTTP fail-closed, auditoría de seguridad y separación de sinks, terminación TLS y flags de cookies, tests de integración/end-to-end con Testcontainers, el frontend SPA completo (cliente REST, layout, i18n, responsive) y la Extensión Autorizada.

### Estado actual verificado en el repositorio

- `backend/` es ya un **único módulo Maven** con `spring-boot-starter-parent` 4.x sobre Java 21, con los paquetes `art`, `boff`, `iam`, `notif`, `observability`, `rest`, `rs`, `security`, `shared`, `wspr` y su `package-info.java`.
- El paquete `shared` está implementado (identificadores UUID v7, tipos primitivos, `Result`, `AccessError`, enums compartidos) con sus unit tests (`IdTest`, `EnumsTest`, `ResultTest`).
- Existe un **walking skeleton** heredado del spec `local-dev-environment` que las tareas de Etapa 1 deben **refactorizar y completar**, no duplicar: `iam.User`/`UserRepository`, `wspr.Workspace`/`WorkspaceRepository`/`Project`/`ProjectRepository`, `rs.ReviewSession`/`ReviewSessionRepository`/`ReviewSessionService`, `rest.ReviewSessionController` + `rest.dto.ReviewSessionDto`, y migraciones Flyway `V1`–`V4`. Este esqueleto carece de fachadas públicas, de `Invitation`, de `canAccessProject` y de autorización real.
- `docs/adr/` existe con la plantilla `0000-template.md` y los ADR `0001` y `0002` del spec previo. Los ADRs de este spec se numeran a partir de `0003`.
- `frontend/` está vacío (sólo `.gitignore`): todo el trabajo de SPA y extensión pertenece a la Etapa 2.

Todos los tests basados en propiedades usan **jqwik** en el backend y **fast-check** en el frontend/extensión, con un mínimo de **100 iteraciones** cada uno y etiqueta identificatoria `Feature: reviews-platform-foundation, Property N`, según lo definido en `Testing Strategy` del documento de diseño. Las 20 propiedades de corrección se reparten así: **Etapa 1** → propiedades 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 17, 19; **Etapa 2** → propiedades 9, 15, 16, 18, 20.

---

# Etapa 1 — Core de dominio (prioridad)

Objetivo: núcleo de dominio construible y ejercitable módulo por módulo, con fachadas públicas, lógica de dominio y property tests, sin dependencias de seguridad HTTP ni de frontend.

## Tasks

- [ ] 1. Cerrar el scaffolding mínimo del monolito Spring Boot
  - [x] 1.1 Consolidar el backend a un único módulo Maven en `backend/`
    - Confirmar que `backend/pom.xml` hereda directamente de `spring-boot-starter-parent` (Spring Boot 4.x, Java 21) sin declarar `<modules>` (monolito de un único módulo Maven)
    - Migrar el árbol de fuentes desde `backend/app/src/` a `backend/src/` (bootstrap `ApiReviewApplication.java`, `application.yaml`, `db/migration/`, tests existentes) preservando la ruta de paquetes `io.github.bsidedevs.api_review`
    - Eliminar la carpeta `backend/app/` una vez migrado el contenido
    - Eliminar las carpetas legadas del layout multi-módulo previo que sólo contienen `target/` de builds anteriores: `backend/art-api/`, `backend/art-impl/`, `backend/boff-api/`, `backend/boff-impl/`, `backend/iam-api/`, `backend/iam-impl/`, `backend/notif-api/`, `backend/notif-impl/`, `backend/observability/`, `backend/rest-api/`, `backend/rs-api/`, `backend/rs-impl/`, `backend/security/`, `backend/shared-kernel/`, `backend/test-fixtures/`, `backend/wspr-api/`, `backend/wspr-impl/`
    - Crear la estructura de paquetes vacíos bajo `backend/src/main/java/io/github/bsidedevs/api_review/`: `iam`, `wspr`, `rs`, `art`, `notif`, `boff` (dominios) y `shared`, `rest`, `observability`, `security` (transversales); cada paquete lleva un `package-info.java` que documenta su responsabilidad y su rol en el grafo de fachadas
    - Configurar las dependencias base comunes a todos los dominios en `backend/pom.xml`: `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-database-postgresql`, driver `postgresql` y `spring-boot-starter-webmvc-test`
    - Verificar que `./mvnw compile` y `./mvnw test` se ejecutan correctamente desde `backend/` tras la migración
    - _Requirements: 19.1, 19.2, 19.3_

  - [ ]* 1.2 Añadir dependencias de testing de fundación y esqueleto de tests de arquitectura
    - Añadir a `backend/pom.xml` (scope `test`): `com.tngtech.archunit:archunit-junit5` y `net.jqwik:jqwik`
    - Definir en `backend/src/test/java/io/github/bsidedevs/api_review/architecture/` una clase de tests de arquitectura vacía; las reglas concretas sobre el grafo de paquetes se añaden en la tarea 16.2
    - Verificar que `./mvnw test` sigue en verde con las nuevas dependencias
    - _Requirements: 19.1, 19.2, 16.3_

- [x] 2. Implementar paquete `shared` (identificadores y tipos primitivos)
  - [x] 2.1 Implementar identificadores opacos y tipos primitivos
    - Bajo `io.github.bsidedevs.api_review.shared`, definir `UserId`, `WorkspaceId`, `ProjectId`, `ReviewSessionId`, `ArtifactId`, `InvitationId`, `NotificationId`, `SessionId`, `CorrelationId` como wrappers value-typed sobre UUID v7
    - Implementar generador de UUID v7 (ordenable + único)
    - Definir `Instant`, `EmailAddress` validado por RFC 5322, `PayloadRef` (sealed: `InlineJson` | `BlobUrl`)
    - Los identificadores son opacos: sin exponer estructura interna más allá de la igualdad
    - _Requirements: 19.1, 19.2_

  - [x] 2.2 Implementar tipos `Result`, `AccessError` y utilitarios compartidos
    - Definir `Result<T, E>` (`Ok` / `Err`) para propagación funcional de errores dentro del paquete `shared`
    - Definir `AccessError = { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND }`
    - Definir enums compartidos: `UserRole`, `ReviewSessionState`, `InvitationStatus`, `ArtifactKind`, `NotificationKind`, `DeliveryStatus`, `PersistenceStatus`, `AccessReason`
    - _Requirements: 8.1, 9.6, 11.4, 18.7, 19.1_

  - [x]* 2.3 Escribir unit tests del paquete `shared`
    - Igualdad y estabilidad de identificadores opacos (`IdTest`)
    - Validación RFC 5322 en `EmailAddress` (`IdTest`)
    - Ordenamiento por generación de UUID v7 (`IdTest`)
    - Cobertura de `Result` (`ResultTest`) y de los enums compartidos (`EnumsTest`)
    - _Requirements: 19.1_

- [ ] 3. Observabilidad mínima necesaria para el core (paquete `observability`)
  - [ ] 3.1 Implementar filtro de correlación y propagación por MDC
    - Filtro servlet en `io.github.bsidedevs.api_review.observability` que lee `X-Correlation-Id` del request o genera uno nuevo (usando `shared.CorrelationId`)
    - Coloca el `CorrelationId` en el MDC de logging y lo emite en la respuesta HTTP
    - Propaga el `CorrelationId` a workers de background mediante utilitario en `shared`
    - _Requirements: 17.5, 17.1_

  - [ ] 3.2 Configurar logging estructurado JSON con correlation
    - Configurar Logback (o equivalente) con encoder JSON que incluya `timestamp`, `level`, `message`, `correlationId`, `userId`, `module`, `event`
    - Alcance de Etapa 1: sink operativo únicamente. La **separación del sink de seguridad** (retenciones independientes, exclusión de datos sensibles) se difiere a la tarea 20.2, porque sólo tiene consumidores reales cuando existen eventos de autenticación y autorización
    - _Requirements: 17.1, 17.5_

  - [ ] 3.3 Implementar `recordError` con sink primario + fallback
    - Sink primario a stdout; fallback a fichero local
    - Si el primario falla, intentar fallback en paralelo sin bloquear la operación original
    - Si ambos fallan, continuar la operación original sin registrar el error
    - Se mantiene en Etapa 1 por ser barato y necesario para diagnosticar la lógica de dominio
    - _Requirements: 17.3, 17.4_

  - [ ] 3.4 Exponer métricas de operación consultables
    - Actuator ya está en el classpath; habilitar el endpoint `/actuator/prometheus` (o equivalente) en `application.yaml` sin acoplarse a un vendor específico
    - _Requirements: 17.2, 19.3_

  - [ ]* 3.5 Property test: universalidad del correlation ID
    - **Property 17: Universalidad del identificador de correlación**
    - Generar peticiones aleatorias (con y sin header `X-Correlation-Id`) sobre un slice MockMvc (sin Testcontainers) y verificar que todos los logs emitidos durante el procesamiento comparten el mismo `correlationId` presente en la respuesta
    - **Validates: Requirements 17.5, 17.1**

  - [ ]* 3.6 Unit tests del fallback de registro de errores
    - Caso primario OK
    - Caso primario falla + fallback OK
    - Caso primario y fallback fallan: la operación continúa
    - _Requirements: 17.3, 17.4_

- [ ] 4. Framework de manejo de errores (Problem+JSON)
  - [ ] 4.1 Implementar `Problem+JSON` (RFC 7807) como formato de error
    - Body con `type`, `title`, `status`, `detail`, `correlationId` y campos específicos por categoría (`errors[]`, `currentState`, `reason`)
    - Reside en el paquete `rest` bajo `io.github.bsidedevs.api_review.rest`
    - Necesario en Etapa 1 para que los contratos de dominio expongan errores coherentes desde el primer módulo
    - _Requirements: 17.5_

  - [ ] 4.2 Implementar `@ControllerAdvice` global con mapeo por categoría
    - Mapear las siete categorías del diseño (400 / 401 / 403 / 404 / 409 / 422 / 500) según la tabla `Error Handling`
    - Prohibir handlers que devuelvan 200 con body de error
    - Ruta sin handler explícito cae a 500 con `correlationId` (nunca mensaje crudo de excepción)
    - _Requirements: 18.5, 18.7_

  - [ ]* 4.3 Unit tests del mapeo de errores por categoría
    - Un test por cada categoría verificando código HTTP, cuerpo y presencia de `correlationId`
    - _Requirements: 17.5, 18.5, 18.7_

- [ ] 5. Paquete `iam`: fachada e identidad (sin credenciales ni Spring Security)
  - [ ] 5.1 Definir la fachada pública del paquete `iam`
    - Interfaces con anotaciones Spring (`@Service` / `@Component`) expuestas como beans desde `io.github.bsidedevs.api_review.iam`
    - Contratos: `UserRole`, `UserIdentity`, `AuthenticatedPrincipal`
    - Declarar en el contrato la superficie completa de la fachada (`authenticate`, `resolveSession`, `revokeSession`, `hasRole`, `getUser`, `listUsersByRole`), implementando en Etapa 1 sólo las consultas (`hasRole`, `getUser`, `listUsersByRole`); `authenticate`, `resolveSession` y `revokeSession` quedan como **punto de extensión declarado** que la tarea 19.2 implementa
    - _Requirements: 4.1, 5.1, 6.1, 6.2_

  - [ ] 5.2 Implementar el modelo de identidad y las consultas de rol y usuario
    - Refactorizar `iam.User`/`UserRepository` existentes para respaldar `UserIdentity` con identificadores de `shared` (`UserId`, `EmailAddress`) y rol inmutable
    - Implementar `hasRole`, `getUser`, `listUsersByRole` (paginado) como beans internos detrás de la fachada
    - Sin hashing de credenciales: la columna de credenciales y su algoritmo se abordan en la tarea 19.1
    - _Requirements: 6.1, 6.2, 19.1_

  - [ ] 5.3 Implementar el proveedor sustituible de `AuthenticatedPrincipal` (costura de seguridad)
    - Definir en `iam` una interfaz `AuthenticatedPrincipalProvider` (nombre a discreción) como **única** fuente del `AuthenticatedPrincipal` para todos los dominios aguas abajo
    - Etapa 1: implementación por defecto que resuelve el principal desde una fuente sustituible (stub/fixture de test o cabecera de desarrollo), documentando en Javadoc que es un punto de costura temporal
    - Etapa 2: la tarea 19.3 sustituye esta implementación por la resolución real vía Spring Security sin cambiar el contrato ni los consumidores
    - Ningún dominio construye un `AuthenticatedPrincipal` por su cuenta
    - _Requirements: 18.1, 19.1, 19.2_

  - [ ]* 5.4 Unit tests de las consultas de `iam`
    - `hasRole` para los tres roles, `getUser` presente/ausente, `listUsersByRole` con paginación y listado vacío
    - Sustitución del proveedor de principal por un doble de test
    - _Requirements: 6.1, 6.2_

- [ ] 6. Paquete `wspr`: dominio de Workspace/Proyecto/Invitación y `canAccessProject`
  - [ ] 6.1 Definir la fachada pública del paquete `wspr`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.wspr`
    - Contratos: `Workspace`, `Project`, `Invitation`, `InvitationStatus`, `AccessDecision`, `AccessReason`
    - Consultas expuestas: `isProjectOwner`, `hasAcceptedInvitation`, `canAccessProject`, `getProject`, `listProjectsOwnedBy`, `listProjectsAccessibleTo`
    - Comandos expuestos: `createProject`, `deleteProject`, `inviteClient`, `acceptInvitation`, `revokeInvitation`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 11.1, 11.2, 11.5_

  - [ ] 6.2 Implementar el dominio `wspr` con la máquina de estados de `Invitation`
    - Extender las entidades JPA existentes `Workspace` y `Project` y añadir `Invitation` con su migración Flyway (clases internas del paquete `wspr`)
    - Máquina de estados `Pending → Accepted | Revoked | Expired`; sin re-aceptación de `Revoked`; sin transiciones de retorno
    - Invariantes estructurales: `Project.workspaceId` NOT NULL y FK válida, `Project.ownerAdminId` NOT NULL y con `role = PROJECT_ADMIN` (verificado vía fachada de `iam`)
    - _Requirements: 2.2, 2.3, 3.1, 3.3, 3.5, 11.5_

  - [ ] 6.3 Implementar `canAccessProject` como lógica de dominio {rol + relación}
    - Bean público de la fachada `wspr`, orquestando `iam` + estado interno de `wspr`; **no** es un filtro HTTP ni depende de Spring Security
    - Evaluar `PROJECT_ADMIN + ownership` **o** `CLIENT + accepted invitation + not blocked`
    - Retornar `AccessDecision` con `granted` + `reason` clasificada, con corto-circuito y ordenamiento explícito de razones de denegación según el diseño
    - Fail-closed a nivel de dominio: cualquier condición no evaluable produce `granted = false`
    - Consumir el `AuthenticatedPrincipal` provisto por la costura de la tarea 5.3
    - _Requirements: 3.4, 3.5, 4.2, 4.3, 4.4, 4.7, 4.8, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.3, 11.1, 11.2, 11.3, 11.5_

  - [ ]* 6.4 Property test: unicidad y no nulidad del propietario del Proyecto
    - **Property 2: Unicidad de propietario del Proyecto**
    - Generar `Project` aleatorios y verificar que siempre existe exactamente un `UserIdentity` con `role = PROJECT_ADMIN` referenciado como `ownerAdminId` durante toda la vida del Proyecto
    - **Validates: Requirements 3.1**

  - [ ]* 6.5 Property test: decisión unificada de autorización (doble llave)
    - **Property 7: Decisión de autorización unificada (doble llave)**
    - Usar `arbAccessScenario` para cubrir todas las combinaciones (rol × propiedad × estado de invitación × bloqueo de proyecto); verificar la equivalencia con la expresión formal del `Modelo 5`
    - **Validates: Requirements 3.4, 3.5, 4.2, 4.3, 4.4, 4.7, 4.8, 4.9, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.3, 11.1, 11.2, 11.5**

  - [ ]* 6.6 Property test: exclusión del Administrador de Plataforma respecto del contenido
    - **Property 8: Exclusión del Administrador de Plataforma respecto del contenido**
    - Para `PLATFORM_ADMIN` aleatorios y proyectos aleatorios, verificar que toda operación sobre contenido de Review Sessions es denegada
    - **Validates: Requirements 6.5, 6.6, 11.3**

- [ ] 7. Checkpoint - bloque `iam` + `wspr`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Paquete `rs`: Review Session y máquina de estados completa
  - [ ] 8.1 Definir la fachada pública del paquete `rs`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.rs`
    - Contratos: `ReviewSessionState`, `PersistenceStatus`, `ReviewSession`
    - Consultas expuestas: `getReviewSession`, `listActiveSessions`, `listAllSessions`, `currentState`
    - Comandos expuestos: `createSession`, `startRecording`, `completeRecording`, `reopen`, `archive`, `delete`
    - Refactorizar `ReviewSessionService` existente para quedar detrás de la fachada (no como punto de entrada directo del controlador)
    - _Requirements: 4.4, 4.5, 4.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1_

  - [ ] 8.2 Implementar máquina de estados y matriz de transiciones válidas
    - Completar la máquina de estados existente en el agregado `ReviewSession`: estado inicial `DRAFT`; `DELETED` absorbente
    - Toda transición no válida retorna error de dominio sin modificar estado
    - `RECORDING → COMPLETED` requiere `persistenceStatus = OK`
    - `reopen`, `archive` y `delete` exigen `PROJECT_ADMIN` propietario verificado, resuelto vía `wspr.canAccessProject` y el proveedor de principal de la tarea 5.3
    - _Requirements: 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.11_

  - [ ] 8.3 Completar persistencia JPA y migraciones Flyway de `rs`
    - Tabla `review_sessions` (ya creada en `V4`): añadir migración que restrinja `state` al enum, materialice `persistence_status` y proteja la asociación a `Project` (FK NOT NULL, sin `UPDATE` de `project_id`)
    - Repositorio interno del paquete `rs` con consultas por proyecto y por estado (activas vs. todas)
    - _Requirements: 7.1, 8.1, 10.4_

  - [ ] 8.4 Implementar tracking de `persistenceStatus` y bloqueo aislado por sesión
    - Marcar `persistenceStatus = FAILED` cuando la persistencia de artefactos falla al finalizar la captura
    - Operaciones subsiguientes sobre esa sesión responden `409` con `reason: PERSISTENCE_PENDING`
    - Ninguna otra sesión resulta afectada
    - Definir job de reconciliación (esqueleto) que reintenta la persistencia y libera la sesión al éxito
    - _Requirements: 10.2_

  - [ ]* 8.5 Property test: buena-formación del estado
    - **Property 3: Buena-formación del estado de la Review Session**
    - Generar sesiones aleatorias y secuencias de comandos; verificar que `state` siempre pertenece a `{DRAFT, RECORDING, COMPLETED, REOPENED, ARCHIVED, DELETED}` y nunca es `NULL` ni admite dos valores simultáneos
    - **Validates: Requirements 8.1**

  - [ ]* 8.6 Property test: estado inicial DRAFT
    - **Property 4: Estado inicial DRAFT**
    - Para todo usuario autorizado y todo Proyecto, tras `createSession` exitoso, la sesión resultante tiene `state = DRAFT`
    - **Validates: Requirements 8.2**

  - [ ]* 8.7 Property test: validez de las transiciones de estado
    - **Property 5: Validez de las transiciones de estado**
    - Generar `(S, comando, actor)` aleatorios sobre la matriz completa (incluyendo intentos desde `DELETED`); verificar que la transición se aplica si y sólo si la terna pertenece al conjunto válido y que en caso contrario el estado permanece en `S` con error de transición inválida
    - **Validates: Requirements 4.5, 4.6, 7.4, 7.5, 7.6, 8.3, 8.4, 8.5, 8.6, 8.7, 8.11**

  - [ ]* 8.8 Property test: operaciones habilitadas por estado
    - **Property 6: Operaciones habilitadas por estado**
    - Para cada par `(estado, categoría de operación)`, verificar que la operación se acepta si y sólo si el par está habilitado en la tabla de operaciones-por-estado
    - **Validates: Requirements 8.8, 8.9, 8.10, 8.11**

- [ ] 9. Checkpoint - bloque `rs`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Paquete `art`: fachada + agregado de artefactos
  - [ ] 10.1 Definir la fachada pública del paquete `art`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.art`
    - Contratos: `ArtifactKind` (taxonomía completa), `Artifact` con `authorUserId`, `createdAt`, `payloadRef`, `metadata`
    - Consultas expuestas: `listArtifacts`, `getArtifact`
    - Captura expuesta: `captureArtifact`
    - Anotaciones expuestas: `addComment`, `replyToComment`, `addTextNote`, `addVoiceNote`
    - Modificación/eliminación de anotaciones propias: `updateOwnAnnotation`, `deleteOwnAnnotation`
    - _Requirements: 1.2, 9.1, 9.2, 9.3, 9.4, 9.7, 9.8, 10.5_

  - [ ] 10.2 Implementar el paquete `art` con autoría y timestamp inmutables
    - Persistir `Artifact` con FK NOT NULL a `review_sessions` y a `users` (autor) mediante migración Flyway (clases internas del paquete `art`)
    - `author_user_id` y `created_at` son inmutables tras la creación (columnas `UPDATE`-protegidas por trigger o por el repositorio)
    - Reglas de familia: `CaptureArtifact` sólo en `RECORDING`; `AnnotationArtifact` en `RECORDING | COMPLETED | REOPENED` (estado consultado vía fachada de `rs`)
    - Modificación/eliminación de `COMMENT`, `TEXT_NOTE`, `VOICE_NOTE` restringida al `authorUserId`
    - Autorización de acceso al contenido vía `wspr.canAccessProject` (lógica de dominio, sin filtro HTTP)
    - _Requirements: 1.2, 8.8, 8.9, 9.1, 9.2, 9.3, 9.4, 10.5_

  - [ ]* 10.3 Property test: invariante estructural de la jerarquía
    - **Property 1: Invariante estructural de la jerarquía**
    - Generar entidades aleatorias del dominio y verificar que todo `Project` referencia exactamente un `Workspace` existente, toda `ReviewSession` referencia exactamente un `Project` existente y todo `Artifact` referencia exactamente una `ReviewSession` existente, sin que estas asociaciones puedan modificarse tras la creación
    - **Validates: Requirements 1.2, 2.2, 2.3, 7.1, 10.5**

  - [ ]* 10.4 Property test: inmutabilidad de la autoría
    - **Property 10: Inmutabilidad de la autoría**
    - Para todo `Artifact`, tras operaciones aleatorias de modificación posteriores, `authorUserId` y `createdAt` permanecen sin cambios
    - **Validates: Requirements 9.1, 9.2**

  - [ ]* 10.5 Property test: sólo el autor modifica sus anotaciones
    - **Property 11: Sólo el autor modifica sus anotaciones**
    - Generar artefactos `COMMENT | TEXT_NOTE | VOICE_NOTE` con autores aleatorios y actores aleatorios; verificar que `updateOwnAnnotation` y `deleteOwnAnnotation` se aceptan si y sólo si `actor.userId = artifact.authorUserId`
    - **Validates: Requirements 9.3, 9.4, 9.5**

  - [ ]* 10.6 Property test: visibilidad completa a los participantes autorizados
    - **Property 12: Visibilidad completa a los participantes autorizados**
    - Para toda sesión con conjunto de artefactos aleatorios y todo principal con `canAccessProject` autorizado, `listArtifacts` devuelve exactamente el conjunto asociado, sin filtrar por autoría y sin cruzar sesiones
    - **Validates: Requirements 9.7, 9.8, 1.4**

  - [ ]* 10.7 Property test: round-trip de persistencia de artefactos
    - **Property 13: Round-trip de persistencia de artefactos**
    - Para toda sesión que transiciona a `COMPLETED` con artefactos capturados, cualquier consulta posterior (incluyendo tras reapertura) devuelve el mismo conjunto con misma autoría, contenido y asociación mientras la sesión no esté en `DELETED`
    - Ejercitar el repositorio a través de su abstracción de dominio (doble en memoria); la verificación contra PostgreSQL real se cubre en la tarea 23.1 de Etapa 2
    - **Validates: Requirements 10.1, 10.3, 10.4**

  - [ ]* 10.8 Property test: atomicidad y aislamiento del fallo de persistencia
    - **Property 14: Atomicidad y aislamiento de la persistencia**
    - Simular fallos de persistencia aleatorios en la transición `RECORDING → COMPLETED`; verificar que la sesión afectada queda bloqueada (`persistenceStatus = FAILED`), los artefactos fallidos no establecen asociación y ninguna otra sesión resulta afectada
    - **Validates: Requirements 10.2, 10.5**

- [ ] 11. Checkpoint - bloque `art`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Paquete `notif`: fachada + mapeo determinístico
  - [ ] 12.1 Definir la fachada pública del paquete `notif`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.notif`
    - Contratos: `NotificationKind`, `DeliveryStatus`, `Notification`
    - Puntos de entrada expuestos: `notifyCommentAdded`, `notifyCommentReplied`, `notifySessionCompleted`, `notifyAiProcessingCompleted`
    - Consultas expuestas: `listMyNotifications`, `markAsConsumed`
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 12.2 Implementar el mapeo evento → destinatarios con la regla de consolidación
    - Consumir las fachadas de `iam`, `wspr`, `rs`, `art` para resolver destinatarios (clases internas del paquete `notif`)
    - `commentAdded` → notifica al Administrador de Proyecto propietario
    - `commentReplied` → autor original + Administrador propietario; si coinciden, **una única** notificación consolidada
    - `sessionCompleted` → todos los participantes autorizados del Proyecto
    - `aiProcessingCompleted` → Administrador de Proyecto propietario
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

  - [ ] 12.3 Implementar política de reintento del registro (hasta 3 intentos)
    - 1 intento original + 2 reintentos con backoff
    - Tras 3 fallos, marcar `deliveryStatus = FAILED` (visible en Backoffice para diagnóstico)
    - _Requirements: 12.5, 12.6_

  - [ ]* 12.4 Property test: determinismo del mapeo evento → destinatarios
    - **Property 19: Determinismo del mapeo evento → destinatarios de notificación**
    - Generar eventos aleatorios (comentario, respuesta, sesión completada, IA finalizada) con actores/propietarios aleatorios; verificar el conjunto exacto de notificaciones según `Modelo 6` y la consolidación 1-única para respuestas donde autor original = propietario
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4**

  - [ ]* 12.5 Unit tests del reintento del registro
    - Éxito en el 1º, 2º y 3º intento; fallo tras 3 → `deliveryStatus = FAILED`
    - _Requirements: 12.6_

- [ ] 13. Paquete `boff`: fachada + consultas administrativas
  - [ ] 13.1 Definir la fachada pública del paquete `boff`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.boff`
    - Contratos: `PlatformMetrics`
    - Consultas expuestas: `listProjectAdmins`, `listClients`, `getMetrics`
    - Cerradas al rol `PLATFORM_ADMIN`, verificado en Etapa 1 como precondición de dominio dentro de la propia fachada (el filtro HTTP equivalente llega en la tarea 20.1)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [ ] 13.2 Implementar consultas agregadas sin exposición de contenido
    - Consumir las fachadas de `iam`, `wspr`, `rs` (clases internas del paquete `boff`)
    - Contadores (totales por rol, workspaces, proyectos, sesiones por estado)
    - Listado paginado de administradores y clientes registrados
    - Ninguna operación devuelve payload de artefactos de Review Sessions
    - Listados vacíos responden colección vacía (no error)
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 11.3_

  - [ ]* 13.3 Unit tests del Backoffice
    - Listados vacíos responden `200` con colección vacía
    - Cualquier intento de acceso a contenido de Review Session desde Backoffice es rechazado
    - _Requirements: 6.3, 6.5, 6.6, 11.3_

- [ ] 14. Checkpoint - bloques `notif` y `boff`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15. Capa REST base + publicación de OpenAPI (paquete `rest`)
  - [ ] 15.1 Configurar capa REST base con Spring MVC sobre las fachadas de dominio
    - Reside en `io.github.bsidedevs.api_review.rest`; base path `/api/v1`
    - Traducción HTTP → llamadas a las fachadas públicas de dominio (sin acceso directo a clases internas); migrar `ReviewSessionController` para invocar la fachada de `rs` en lugar del servicio interno
    - Cadena de filtros de Etapa 1: `correlationFilter` → handler. Documentar en el código (Javadoc de la configuración de la cadena) los **puntos de extensión** `authenticationFilter` y `authorizationFilter` que insertan las tareas 19.3 y 20.1
    - El `AuthenticatedPrincipal` se obtiene del proveedor sustituible de la tarea 5.3
    - Serialización de errores como `Problem+JSON`
    - Códigos por categoría (`200/201/204/400/401/403/404/409/422/5xx`) según diseño
    - _Requirements: 13.1, 17.5, 18.1, 18.2_

  - [ ] 15.2 Configurar generación automática de OpenAPI (springdoc-openapi)
    - Anotar contratos de fachada y controladores para reflejar todas las operaciones expuestas
    - La generación de la especificación no interrumpe el tráfico HTTP en curso
    - _Requirements: 13.2, 13.3, 13.4, 13.5, 13.6_

  - [ ] 15.3 Exponer la especificación OpenAPI en endpoints HTTP
    - `GET /api/v1/openapi.json`, `GET /api/v1/openapi.yaml`
    - `GET /api/v1/docs` (Swagger UI opcional)
    - Servir la especificación previa aunque falle la regeneración
    - El test de integración de este comportamiento se ejecuta en la tarea 23.2 (Etapa 2)
    - _Requirements: 13.4, 13.5, 13.6, 13.7_

- [ ] 16. Test fixtures y reglas de arquitectura
  - [ ]* 16.1 Implementar el paquete `test-fixtures` bajo `src/test/java`
    - Ubicado en `backend/src/test/java/io/github/bsidedevs/api_review/testfixtures/`, compartido por todos los tests del monolito (no es un módulo Maven separado)
    - Generadores jqwik `arbUser(role)`, `arbProject(ownerAdminId)`, `arbReviewSessionInState(state)`, `arbAccessScenario`, `arbArtifactKind`
    - Incluir el doble de `AuthenticatedPrincipalProvider` usado por los property tests de dominio (costura de la tarea 5.3)
    - Los generadores equivalentes con fast-check viven en el proyecto `frontend/` (creado en la tarea 24.1)
    - _Requirements: 16.3_

  - [ ] 16.2 Añadir reglas ArchUnit sobre el grafo de dependencias de paquetes
    - Los paquetes de dominio no importan clases marcadas como internas de otro dominio (por convención: subpaquetes `.internal.*` no son accesibles desde fuera del dominio propietario)
    - Sólo se importan clases del paquete raíz del dominio proveedor (donde vive la fachada Spring bean)
    - El grafo de dependencias entre dominios es un DAG conforme al diseño: `iam ← wspr ← rs ← art`, `notif → {rs, art, wspr, iam}`, `boff → {iam, wspr, rs}`
    - El paquete `shared` es importable por cualquier dominio; no depende de ningún dominio
    - La capa `rest` no llama directamente a clases marcadas como internas de dominio; sólo a fachadas
    - _Requirements: 19.1, 19.2_

  - [ ] 16.3 Regla ArchUnit para exclusiones MVP
    - Añadir en la clase de tests de arquitectura (tarea 1.2) una regla ArchUnit que verifique que las funcionalidades explícitamente excluidas del MVP no tienen código que las implemente
    - Regla 1: no existe clase de producción relacionada con edición colaborativa en tiempo real — detectar clases cuyo nombre simple contenga `Realtime`, `Collaboration` o `Websocket` dentro de los paquetes de dominio (`io.github.bsidedevs.api_review.iam..`, `wspr..`, `rs..`, `art..`, `notif..`, `boff..`)
    - Regla 2: no existe código de compartición pública de sesiones — detectar clases con `PublicShare` o `SessionShare` en el nombre dentro de los paquetes de dominio
    - Regla 3: no existe integración con Jira ni con GitHub — detectar clases con `Jira` o `GitHub` en el nombre dentro de los paquetes de dominio
    - La regla falla el build si detecta alguna exclusión implementada, hasta que el spec correspondiente la incorpore explícitamente
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6_

- [ ] 17. Architecture Decision Records de fundación
  - [x] 17.1 Crear estructura `docs/adr/` con plantilla estándar
    - Plantilla `docs/adr/0000-template.md` con secciones: Contexto, Alternativas consideradas, Decisión, Consecuencias, Referencias
    - _Requirements: 20.1, 20.2_

  - [ ] 17.2 ADR-0003: Monolito Spring Boot con paquetes por dominio
    - Contexto: se optó por un monolito Spring Boot simple con paquetes por dominio dentro de un único módulo Maven para minimizar la complejidad inicial del MVP
    - Alternativas consideradas: (a) microservicios desde el inicio, (b) monolito modular con módulos Maven separados `*-api`/`*-impl`, (c) monolito plano sin fronteras internas
    - Decisión: monolito de un único módulo Maven con paquetes por dominio y fronteras internas materializadas por convención (paquetes `internal.*` privados al dominio) + ArchUnit
    - Consecuencias: las fronteras entre dominios se preservan mediante disciplina de código y reglas ArchUnit, no mediante fronteras de build; la extracción a servicios independientes queda fuera del alcance MVP (Requirement 19 fue relajado)
    - _Requirements: 19.1, 19.2, 20.1_

  - [ ] 17.3 ADR-0004: Elección de bibliotecas PBT (jqwik + fast-check)
    - Justificar la elección y sus alternativas
    - _Requirements: 20.1_

  - [ ] 17.4 ADR-0005: Esquema de identificadores (UUID v7)
    - Justificar UUID v7 (ordenable + único) frente a UUID v4 y snowflake
    - _Requirements: 20.1_

  - [ ] 17.5 ADR-0006: Estrategia inicial de almacenamiento de payloads
    - `bytea` con TOAST en primera iteración MVP vs. object storage S3-compatible
    - _Requirements: 20.1_

- [ ] 18. Checkpoint final de Etapa 1 - core de dominio completo
  - Ensure all tests pass, ask the user if questions arise.

---

# Etapa 2 — Seguridad aplicada, end-to-end y frontend (diferida)

**Dependencia**: toda esta etapa depende del cierre de la Etapa 1 (tarea 18). Las fachadas de dominio, `canAccessProject` y la cadena REST base deben existir antes de aplicar seguridad HTTP, tests end-to-end y clientes.

- [ ] 19. Credenciales y autenticación real
  - [ ] 19.1 Implementar hashing de credenciales resistente a fuerza bruta
    - Adoptar bcrypt o argon2 (decisión concreta en ADR-0007, tarea 26.1) dentro del paquete `security`
    - Rechazar almacenamiento de credenciales en claro; migración Flyway con columnas hash + salt sobre la tabla de usuarios de `iam`
    - _Requirements: 18.4_

  - [ ] 19.2 Implementar `authenticate`, `resolveSession` y `revokeSession` en `iam`
    - Completar los puntos de extensión declarados en la fachada de `iam` (tarea 5.1)
    - Persistencia de `Session` y `SecurityAuditEvent` en PostgreSQL vía Flyway (clases internas del paquete `iam`)
    - Registrar cada intento (éxito y fallo) en el log de seguridad con `correlationId`
    - _Requirements: 18.1, 18.4, 18.5, 18.6_

  - [ ] 19.3 Implementar el filtro de autenticación integrado con Spring Security
    - Añadir `spring-boot-starter-security` y configurar la cadena de filtros en el paquete `security`
    - Resolver el `AuthenticatedPrincipal` a partir del token/cookie de sesión invocando la fachada de `iam`, **sustituyendo** la implementación stub del proveedor de la tarea 5.3 sin cambiar su contrato ni sus consumidores
    - Insertar el `authenticationFilter` en el punto de extensión documentado en la tarea 15.1
    - Peticiones sin credencial válida → 401 antes de alcanzar cualquier handler de dominio
    - _Requirements: 18.1, 18.5_

  - [ ]* 19.4 Property test: universalidad de la autenticación
    - **Property 15: Universalidad de la autenticación**
    - Generar rutas de dominio aleatorias y credenciales válidas/inválidas/ausentes; verificar que sólo las peticiones con `AuthenticatedPrincipal` resuelto alcanzan el handler de dominio
    - **Validates: Requirements 18.1**

  - [ ]* 19.5 Property test: universalidad de la auditoría de seguridad
    - **Property 18: Universalidad de la auditoría de seguridad**
    - Generar peticiones aleatorias con resultados de auth (éxito/fallo, ambas etapas); verificar que se emite exactamente un evento de auditoría con `correlationId`, `userId` (si conocido), resultado y motivo
    - **Validates: Requirements 18.5, 18.6**

- [ ] 20. Filtro global de autorización HTTP (fail-closed)
  - [ ] 20.1 Implementar filtro de autorización con corto-circuito en DENY
    - Reside en el paquete `security` bajo `io.github.bsidedevs.api_review.security`, invocando la fachada de `wspr` (`canAccessProject`) implementada en la tarea 6.3
    - Insertar el `authorizationFilter` en el punto de extensión documentado en la tarea 15.1
    - Aplicado a toda operación de dominio (lectura, escritura, consulta) sin distinguir entre exposición y modificación
    - En cuanto un control retorna `DENY`, no se evalúan los siguientes
    - Fail-closed: si la decisión no se computa (error, ruta olvidada, configuración), la petición se deniega con 403 (o 404 si evita revelar existencia)
    - Registrar denegaciones en log de seguridad aunque el registro falle por razones técnicas
    - _Requirements: 9.6, 11.4, 18.2, 18.5, 18.7_

  - [ ] 20.2 Implementar auditoría en logs de seguridad y separación de sinks
    - Completar la configuración de logging de la tarea 3.2 separando el sink de log de seguridad del operativo (retenciones independientes, sin datos sensibles)
    - Emitir eventos de auditoría de autenticación y autorización (éxito y fallo) con `correlationId`, `userId`, resultado y motivo
    - _Requirements: 18.5, 18.6_

  - [ ]* 20.3 Property test: fail-closed (la denegación siempre gana)
    - **Property 9: Autorización fail-closed (la denegación siempre gana)**
    - Generar operaciones aleatorias con listas aleatorias de controles concurrentes; verificar que si al menos uno retorna `DENY`, la decisión final es `DENY` con independencia del éxito o fracaso del registro auxiliar
    - **Validates: Requirements 9.6, 11.4, 18.7**

  - [ ]* 20.4 Property test: universalidad de la autorización en toda petición
    - **Property 16: Universalidad de la autorización**
    - Generar rutas de dominio aleatorias (incluyendo rutas sin handler); verificar que siempre se computa una decisión antes del handler y, si no se computa, la respuesta es `DENY`
    - **Validates: Requirements 18.2, 18.7**

- [ ] 21. Transporte seguro
  - [ ] 21.1 Configurar terminación TLS en el proxy y flags de seguridad de cookies
    - Configuración de reverse proxy en `infra/docker/` para TLS
    - Cookies de sesión `Secure` + `HttpOnly` + `SameSite`
    - _Requirements: 18.3_

- [ ] 22. Checkpoint - seguridad aplicada
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 23. Tests de integración y end-to-end
  - [ ]* 23.1 Tests de integración con Testcontainers (PostgreSQL real)
    - Levantar contexto Spring Boot completo contra PostgreSQL 16 real en Testcontainers; ejecutar migraciones Flyway y verificar arranque limpio
    - Verificar round-trip de persistencia de artefactos contra PostgreSQL (complementa la propiedad 13 de la tarea 10.7): crear sesión → capturar artefactos → completar → reapertura → verificar artefactos intactos con misma autoría y asociación
    - Verificar la máquina de estados de Review Session con transacciones reales: todas las transiciones válidas e inválidas producen el estado correcto en base de datos
    - _Requirements: 10.1, 10.3, 10.4, 8.1_

  - [ ]* 23.2 Tests de integración de la especificación OpenAPI
    - Verificar que el endpoint `/api/v1/openapi.json` sirve la especificación vigente aunque falle la regeneración
    - Verificar que la API sigue operativa durante y después de un fallo de actualización de especificación
    - **Property 20 (resiliencia OpenAPI)**: simular fallos de regeneración de la especificación y verificar que la API no deja de responder y que el endpoint de especificación devuelve la última versión publicada satisfactoriamente
    - _Requirements: 13.2, 13.3, 13.4, 13.5, 13.6, 13.7_

  - [ ]* 23.3 Tests end-to-end del flujo principal de revisión
    - Flujo completo contra PostgreSQL en Testcontainers: crear proyecto → invitar cliente → crear sesión → iniciar captura → añadir artefactos → finalizar captura → añadir comentarios → verificar notificaciones → archivar sesión
    - Verificar control de acceso en cada paso: rol correcto + relación correcta autoriza; rol incorrecto o sin relación deniega y registra en log de seguridad
    - Cubrir: caso autorizado (admin propietario), denegado por rol (admin no propietario), denegado por relación (cliente sin invitación aceptada), sin credencial (401)
    - _Requirements: 1.1, 1.2, 4.1, 4.4, 4.5, 5.2, 5.3, 5.4, 8.3, 8.4, 9.1, 9.3, 12.1, 12.3_

- [ ] 24. Frontend SPA (aplicación web)
  - [ ] 24.1 Crear el proyecto frontend (React + TypeScript + Vite)
    - Bootstrap de `frontend/` con Vite + TypeScript + React + Vitest
    - Generar el cliente REST TypeScript desde la especificación OpenAPI del backend
    - Añadir `fast-check` como dependencia de test para los property tests del frontend
    - Configurar tooling común (linter, formatter, paths de alias) sin acoplamiento a un runner de CI específico
    - _Requirements: 13.1_

  - [ ] 24.2 Implementar el cliente REST con manejo de errores
    - Cliente HTTP TypeScript que consume `/api/v1` con `X-Correlation-Id` en request
    - Manejar respuestas de error: 401 → redirigir a login, 403 → mostrar mensaje de acceso denegado, 404 → página no encontrada, 409 → conflicto de estado, 422 → errores de validación, 5xx → error genérico con `correlationId`
    - _Requirements: 13.1, 13.7_

  - [ ] 24.3 Implementar layout base, routing e i18n
    - Layout base con enrutado inicial y páginas placeholder por rol (`ProjectAdmin`, `Client`, `PlatformAdmin`)
    - Externalizar todos los textos en archivos de recursos i18n en `frontend/src/i18n/{locale}.json`, sin textos literales en el código
    - Selector de idioma en la UI; resolver locale efectivo una única vez por render
    - Si el locale seleccionado tiene todas las claves requeridas → renderizar entero en ese locale
    - Si cualquier clave requerida falta → renderizar entero en el Idioma por Defecto (sin mezcla de idiomas)
    - **Property del frontend: fallback i18n todo-o-nada** — generar bundles aleatorios con claves faltantes y conjuntos requeridos aleatorios; verificar que el render efectivo es enteramente en el locale seleccionado si y sólo si todas las claves están presentes; en caso contrario, enteramente en el locale por defecto
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_

  - [ ] 24.4 Implementar diseño responsive (escritorio y móvil)
    - Grid responsive con breakpoints para desktop y móvil
    - En resoluciones móviles: funcionalidades de consulta y comentario sobre Review Sessions disponibles
    - _Requirements: 15.1, 15.2_

  - [ ] 24.5 Checkpoint - frontend SPA completo
    - Ensure all tests pass, ask the user if questions arise.

- [ ] 25. Extensión Autorizada de navegador
  - [ ] 25.1 Crear el proyecto de la Extensión Autorizada
    - Crear `frontend/extension/` con manifest WebExtension (MV3) y permisos mínimos necesarios
    - Estructura de background/service worker + content script + popup UI en TypeScript
    - Reutilizar el cliente REST generado desde OpenAPI del proyecto `frontend/` (cliente compartido)
    - _Requirements: 5.1_

  - [ ] 25.2 Implementar el flujo de captura desde la extensión
    - Inicio y participación en Review Sessions desde la extensión: inicio de sesión, captura de artefactos e integración con la API REST del backend
    - Reutilizar el mecanismo de autenticación de la SPA (mismo endpoint `/api/v1`, misma sesión/token emitido por `iam`)
    - _Requirements: 5.1, 5.2, 8.3, 8.8_

  - [ ] 25.3 Checkpoint - Extensión Autorizada completa
    - Ensure all tests pass, ask the user if questions arise.

- [ ] 26. ADRs de Etapa 2
  - [ ] 26.1 ADR-0007: Elección del algoritmo de hashing de credenciales (bcrypt vs argon2)
    - Contexto, alternativas (bcrypt, argon2id, scrypt), decisión y consecuencias
    - Documentar parámetros de coste elegidos y razón de la elección
    - _Requirements: 18.4, 20.1_

  - [ ] 26.2 ADR-0008: Estrategia de sesión y gestión de tokens
    - Documentar el mecanismo de sesión elegido (cookies vs. JWT vs. tokens opacos), ciclo de vida de tokens, revocación y renovación
    - _Requirements: 18.1, 20.1_

  - [ ] 26.3 ADR-0009: Configuración TLS y flags de cookies
    - Documentar terminación TLS en el proxy, flags `Secure + HttpOnly + SameSite` en cookies de sesión y política de HSTS
    - _Requirements: 18.3, 20.1_

- [ ] 27. Checkpoint final del spec de fundación
  - Ejecutar todos los tests de Etapa 1 (`./mvnw test`) y Etapa 2 (`npm test -- --run` en frontend y extensión)
  - Verificar que el grafo ArchUnit (tareas 16.2 y 16.3) pasa sin errores de dependencias de dominio ni exclusiones MVP implementadas accidentalmente
  - Verificar que las 20 propiedades de corrección pasan con ≥ 100 iteraciones cada una
  - Verificar que la especificación OpenAPI está sincronizada con los endpoints expuestos (`/api/v1/openapi.json` refleja todos los controladores anotados)
  - Verificar que los ADRs 0003–0009 existen y están documentados en `docs/adr/`
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- El plan está dividido en **dos etapas**: Etapa 1 (tareas 1–18) construye el core de dominio módulo por módulo; Etapa 2 (tareas 19–27) aplica seguridad HTTP, pruebas end-to-end y clientes. La numeración es secuencial y continua entre etapas.
- Las tareas marcadas con `*` son **opcionales** (tests unitarios, tests de propiedades e integración) y pueden omitirse en un MVP rápido; sin embargo, según Requirement 16, toda funcionalidad entregada dentro del alcance MVP requiere pruebas escritas como precondición a considerarse terminada.
- Cada tarea referencia sub-cláusulas concretas de los criterios de aceptación en `_Requirements: X.Y_`, garantizando trazabilidad requisito → tarea.
- Las **20 propiedades de corrección** se conservan íntegras y se agrupan junto al código que validan: propiedades 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 17 y 19 en Etapa 1 (lógica de dominio); propiedades 9, 15, 16, 18 y 20 en Etapa 2 (seguridad HTTP aplicada e i18n del frontend).
- Todos los property tests usan como mínimo **100 iteraciones** y llevan la etiqueta `Feature: reviews-platform-foundation, Property N` en el nombre del test, según el diseño.
- Donde el diseño exige un `AuthenticatedPrincipal`, la Etapa 1 lo resuelve mediante el **proveedor sustituible** definido en la tarea 5.3 (stub/fixture de test). La tarea 19.3 reemplaza esa implementación por la resolución real vía Spring Security sin alterar el contrato. Los puntos de costura de la cadena de filtros quedan documentados en la tarea 15.1.
- Los checkpoints (tareas 7, 9, 11, 14, 18, 22, 27) son puntos naturales de validación incremental: uno por bloque de dominio en Etapa 1, más el cierre de seguridad y el cierre de fundación en Etapa 2.
- Existe código heredado del spec `local-dev-environment` (`iam.User`, `wspr.Workspace`/`Project`, `rs.ReviewSession*`, `rest.ReviewSessionController`, migraciones `V1`–`V4`). Las tareas de Etapa 1 lo **refactorizan y completan** detrás de fachadas; no deben duplicarlo.
- Este spec es la **fundación**: la subestructura interna de cada paquete de dominio (subpaquetes `controllers/`, `services/`, `repositories/`, entidades JPA concretas, migraciones Flyway detalladas, jobs de reconciliación completos, etc.) se desarrollará en specs posteriores tomando este documento como marco de referencia. Aquí sólo se materializa la fachada pública de cada dominio y las clases internas mínimas necesarias para satisfacer las propiedades declaradas.

## Task Dependency Graph

Las dependencias entre tareas de alto nivel son:

- Tarea 1 → sin dependencias previas
- Tarea 2 → sin dependencias previas (puede ejecutarse en paralelo con 1)
- Tarea 3 → depende de 2 (usa `shared.CorrelationId`)
- Tarea 4 → depende de 3
- Tarea 5 → depende de 2, 3
- Tarea 6 → depende de 5
- Tarea 7 (checkpoint) → depende de 5, 6
- Tarea 8 → depende de 6
- Tarea 9 (checkpoint) → depende de 8
- Tarea 10 → depende de 8
- Tarea 11 (checkpoint) → depende de 10
- Tarea 12 → depende de 10
- Tarea 13 → depende de 5, 6, 8
- Tarea 14 (checkpoint) → depende de 12, 13
- Tarea 15 → depende de 14
- Tarea 16 → depende de 15
- Tarea 17 → depende de 1 (solo para la plantilla ADR)
- Tarea 18 (checkpoint Etapa 1) → depende de 15, 16, 17
- Tarea 19 → depende de 18
- Tarea 20 → depende de 19
- Tarea 21 → depende de 20
- Tarea 22 (checkpoint seguridad) → depende de 21
- Tarea 23 → depende de 22
- Tarea 24 → depende de 18 (puede ejecutarse en paralelo con 19–22)
- Tarea 25 → depende de 24
- Tarea 26 → depende de 19, 20, 21
- Tarea 27 (checkpoint final) → depende de 23, 25, 26

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.2", "17.2", "17.3", "17.4", "17.5"] },
    { "id": 1, "tasks": ["3.1", "3.2", "3.3", "3.4", "4.1", "5.1", "6.1"] },
    { "id": 2, "tasks": ["4.2", "5.2", "5.3", "6.2", "8.1", "10.1", "12.1", "13.1", "16.1"] },
    { "id": 3, "tasks": ["3.5", "3.6", "4.3", "5.4", "6.3", "8.2", "8.3", "10.2", "12.2", "13.2"] },
    { "id": 4, "tasks": ["6.4", "6.5", "6.6", "8.4", "12.3", "13.3", "15.1"] },
    { "id": 5, "tasks": ["8.5", "8.6", "8.7", "8.8", "10.3", "12.4", "12.5", "15.2"] },
    { "id": 6, "tasks": ["10.4", "10.5", "10.6", "10.7", "10.8", "15.3", "16.2"] },
    { "id": 7, "tasks": ["16.3"] },
    { "id": 8, "tasks": ["19.1"] },
    { "id": 9, "tasks": ["19.2"] },
    { "id": 10, "tasks": ["19.3", "20.1", "24.1"] },
    { "id": 11, "tasks": ["19.4", "19.5", "20.2", "24.2"] },
    { "id": 12, "tasks": ["20.3", "20.4", "21.1", "24.3", "24.4"] },
    { "id": 13, "tasks": ["23.1", "26.1", "26.2", "26.3"] },
    { "id": 14, "tasks": ["23.2", "23.3", "24.5", "25.1"] },
    { "id": 15, "tasks": ["25.2"] }
  ]
}
```
