# Implementation Plan: Reviews Platform Foundation

## Overview

Convert the feature design into a series of prompts for a code-generation LLM that will implement each step with incremental progress. Make sure that each prompt builds on the previous prompts, and ends with wiring things together. There should be no hanging or orphaned code that isn't integrated into a previous step. Focus ONLY on tasks that involve writing, modifying, or testing code.

Este plan implementa el **spec de fundación** de la plataforma Reviews. Su alcance es transversal y **no** desarrolla los detalles internos de cada paquete de dominio (endpoints concretos, DTOs completos, pantallas, migraciones específicas): esos detalles se abordarán en specs posteriores tomando este documento como marco. En cambio, sí materializa:

- El esqueleto de **monolito Spring Boot único** con **paquetes por dominio** bajo `io.github.bsidedevs.api_review` y sus reglas de dependencia (grafo acíclico entre fachadas de dominio, comunicación entre dominios exclusivamente vía fachadas Spring bean).
- El paquete `shared` con identificadores y tipos primitivos comunes.
- Los mecanismos transversales (correlation ID, logging estructurado, error handling, filtros de autenticación y autorización, i18n, publicación de OpenAPI) en los paquetes `observability`, `security` y `rest`.
- Los componentes de dominio explícitamente identificados como fundacionales: la función unificada `canAccessProject` y el agregado Review Session con su máquina de estados.
- Las **fachadas públicas** de los paquetes `art`, `notif` y `boff` (con implementación mínima suficiente para ejercitar las propiedades de este spec y ampliable en specs posteriores).
- El scaffolding del frontend SPA y de la Extensión Autorizada.
- El paquete `test-fixtures` (bajo `src/test/java`), las reglas ArchUnit y los property tests que validan las 20 propiedades de corrección.
- Los ADRs iniciales que documentan las decisiones arquitectónicas relevantes de la fundación.

La subestructura interna de cada paquete de dominio (subpaquetes `controllers/`, `services/`, `repositories/`, entidades JPA, tablas Flyway, controladores) queda deliberadamente **fuera del alcance** de este spec y se decidirá en specs por módulo posteriores. Aquí sólo se materializa la **frontera pública** de cada dominio (interfaces y clases con anotaciones Spring que constituyen su fachada) y las clases internas mínimas necesarias para satisfacer las propiedades declaradas.

Todos los tests basados en propiedades usan **jqwik** en el backend y **fast-check** en el frontend/extensión, con un mínimo de **100 iteraciones** cada uno y etiqueta identificatoria `Feature: reviews-platform-foundation, Property N`, según lo definido en `Testing Strategy` del documento de diseño.

## Tasks

- [x] 1. Establecer estructura del monolito Spring Boot y workspaces
  - [x] 1.1 Consolidar el backend a un único módulo Maven en `backend/`
    - Confirmar que `backend/pom.xml` hereda directamente de `spring-boot-starter-parent` (Spring Boot 4.x, Java 21) sin declarar `<modules>` (monolito de un único módulo Maven)
    - Migrar el árbol de fuentes desde `backend/app/src/` a `backend/src/` (bootstrap `ApiReviewApplication.java`, `application.yaml`, `db/migration/`, tests existentes) preservando la ruta de paquetes `io.github.bsidedevs.api_review`
    - Eliminar la carpeta `backend/app/` una vez migrado el contenido
    - Eliminar las carpetas legadas del layout multi-módulo previo que sólo contienen `target/` de builds anteriores: `backend/art-api/`, `backend/art-impl/`, `backend/boff-api/`, `backend/boff-impl/`, `backend/iam-api/`, `backend/iam-impl/`, `backend/notif-api/`, `backend/notif-impl/`, `backend/observability/`, `backend/rest-api/`, `backend/rs-api/`, `backend/rs-impl/`, `backend/security/`, `backend/shared-kernel/`, `backend/test-fixtures/`, `backend/wspr-api/`, `backend/wspr-impl/`
    - Crear la estructura de paquetes vacíos bajo `backend/src/main/java/io/github/bsidedevs/api_review/`: `iam`, `wspr`, `rs`, `art`, `notif`, `boff` (dominios) y `shared`, `rest`, `observability`, `security` (transversales); cada paquete lleva un `package-info.java` que documenta su responsabilidad y su rol en el grafo de fachadas
    - Configurar las dependencias base comunes a todos los dominios en `backend/pom.xml`: `spring-boot-starter-webmvc` (ya presente), `spring-boot-starter-validation`, `flyway-core` (para próximas tareas), driver `postgresql` y `spring-boot-starter-webmvc-test` (ya presente). Estas dependencias son base común del monolito; las dependencias específicas por dominio se añadirán en los specs por módulo correspondientes
    - Verificar que `./mvnw compile` y `./mvnw test` se ejecutan correctamente desde `backend/` tras la migración
    - _Requirements: 19.1, 19.2, 19.3_
  
  - [ ]* 1.2 Configurar workspaces de frontend y extensión
    - Bootstrap `frontend/` con Vite + TypeScript + React + Vitest
    - Bootstrap `frontend/extension/` (o carpeta equivalente) como paquete de extensión de navegador con manifest WebExtension y TypeScript
    - Configurar tooling común (linter, formatter) sin acoplamiento a un runner de CI específico
    - _Requirements: 5.1, 15.1, 16.3, 19.1_
  
  - [ ]* 1.3 Añadir esqueleto de tests de arquitectura con ArchUnit
    - Añadir dependencia `com.tngtech.archunit:archunit-junit5` (scope `test`) en `backend/pom.xml`
    - Definir en `backend/src/test/java/io/github/bsidedevs/api_review/architecture/` una clase de tests de arquitectura vacía; las reglas concretas sobre el grafo de paquetes se añaden en tarea 17.2
    - _Requirements: 19.1, 19.2_

- [ ] 2. Implementar paquete `shared` (identificadores y tipos primitivos)
  - [ ] 2.1 Implementar identificadores opacos y tipos primitivos
    - Bajo `io.github.bsidedevs.api_review.shared`, definir `UserId`, `WorkspaceId`, `ProjectId`, `ReviewSessionId`, `ArtifactId`, `InvitationId`, `NotificationId`, `SessionId`, `CorrelationId` como wrappers value-typed sobre UUID v7
    - Implementar generador de UUID v7 (ordenable + único)
    - Definir `Instant`, `EmailAddress` validado por RFC 5322, `PayloadRef` (sealed: `InlineJson` | `BlobUrl`)
    - Los identificadores son opacos: sin exponer estructura interna más allá de la igualdad
    - _Requirements: 19.1, 19.2_
  
  - [ ] 2.2 Implementar tipos `Result`, `AccessError` y utilitarios compartidos
    - Definir `Result<T, E>` (`Ok` / `Err`) para propagación funcional de errores dentro del paquete `shared`
    - Definir `AccessError = { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND }`
    - Definir enums compartidos: `UserRole`, `ReviewSessionState`, `InvitationStatus`, `ArtifactKind`, `NotificationKind`, `DeliveryStatus`, `PersistenceStatus`, `AccessReason`
    - _Requirements: 8.1, 9.6, 11.4, 18.7, 19.1_
  
  - [ ]* 2.3 Escribir unit tests del paquete `shared`
    - Igualdad y estabilidad de identificadores opacos
    - Validación RFC 5322 en `EmailAddress`
    - Ordenamiento por generación de UUID v7
    - _Requirements: 19.1_

- [ ] 3. Instrumentación transversal de observabilidad (paquete `observability`)
  - [ ] 3.1 Implementar filtro de correlación y propagación por MDC
    - Filtro servlet en `io.github.bsidedevs.api_review.observability` que lee `X-Correlation-Id` del request o genera uno nuevo
    - Coloca el `CorrelationId` en el MDC de logging y lo emite en la respuesta HTTP
    - Propaga el `CorrelationId` a workers de background mediante utilitario en `shared`
    - _Requirements: 17.5, 17.1_
  
  - [ ] 3.2 Configurar logging estructurado JSON con correlation
    - Configurar Logback (o equivalente) con encoder JSON que incluya `timestamp`, `level`, `message`, `correlationId`, `userId`, `module`, `event`
    - Separar sink de log operativo del sink de log de seguridad (retenciones independientes, sin datos sensibles)
    - _Requirements: 17.1, 17.5, 18.5, 18.6_
  
  - [ ] 3.3 Implementar `recordError` con sink primario + fallback
    - Sink primario a stdout; fallback a fichero local
    - Si el primario falla, intentar fallback en paralelo sin bloquear la operación original
    - Si ambos fallan, continuar la operación original sin registrar el error
    - _Requirements: 17.3, 17.4_
  
  - [ ] 3.4 Exponer métricas de operación consultables
    - Habilitar Spring Boot Actuator + endpoint `/actuator/prometheus` (o equivalente) sin acoplarse a un vendor específico
    - _Requirements: 17.2, 19.3_
  
  - [ ]* 3.5 Property test: universalidad del correlation ID
    - **Property 17: Universalidad del identificador de correlación**
    - Generar peticiones aleatorias (con y sin header `X-Correlation-Id`) y verificar que todos los logs emitidos durante el procesamiento comparten el mismo `correlationId` presente en la respuesta
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
    - _Requirements: 17.5_
  
  - [ ] 4.2 Implementar `@ControllerAdvice` global con mapeo por categoría
    - Mapear las siete categorías del diseño (400 / 401 / 403 / 404 / 409 / 422 / 500) según la tabla `Error Handling`
    - Prohibir handlers que devuelvan 200 con body de error
    - Ruta sin handler explícito cae a 500 con `correlationId` (nunca mensaje crudo de excepción)
    - _Requirements: 18.5, 18.7_
  
  - [ ]* 4.3 Unit tests del mapeo de errores por categoría
    - Un test por cada categoría verificando código HTTP, cuerpo y presencia de `correlationId`
    - _Requirements: 17.5, 18.5, 18.7_

- [ ] 5. Paquete `iam` (identidad, autenticación y auditoría)
  - [ ] 5.1 Definir la fachada pública del paquete `iam`
    - Interfaces con anotaciones Spring (`@Service` / `@Component`) expuestas como beans desde `io.github.bsidedevs.api_review.iam`
    - Contratos: `UserRole`, `UserIdentity`, `AuthenticatedPrincipal`
    - Funciones expuestas por la fachada: `authenticate`, `resolveSession`, `revokeSession`, `hasRole`, `getUser`, `listUsersByRole`
    - _Requirements: 4.1, 5.1, 6.1, 6.2, 18.1_
  
  - [ ] 5.2 Implementar hashing de credenciales resistente a fuerza bruta
    - Adoptar bcrypt o argon2 (decisión concreta en ADR-003) dentro del paquete `security`
    - Rechazar almacenamiento de credenciales en claro; migración inicial de esquema con columnas hash + salt
    - _Requirements: 18.4_
  
  - [ ] 5.3 Implementar el paquete `iam`: repositorio de usuarios, autenticación y sesiones
    - Persistencia de `User`, `Session` y `SecurityAuditEvent` en PostgreSQL vía Flyway (clases internas del paquete `iam`)
    - Implementar `authenticate`, `resolveSession`, `revokeSession` como beans que respaldan la fachada
    - Registrar cada intento (éxito y fallo) en el log de seguridad con `correlationId`
    - _Requirements: 18.1, 18.4, 18.5, 18.6_
  
  - [ ] 5.4 Implementar filtro de autenticación integrado con Spring Security
    - Filtro en el paquete `security` que resuelve el `AuthenticatedPrincipal` a partir del token/cookie de sesión invocando la fachada de `iam`
    - Peticiones sin credencial válida → 401 antes de alcanzar cualquier handler de dominio
    - _Requirements: 18.1, 18.5_
  
  - [ ]* 5.5 Property test: universalidad de la autenticación
    - **Property 15: Universalidad de la autenticación**
    - Generar rutas de dominio aleatorias y credenciales válidas/inválidas/ausentes; verificar que sólo las peticiones con `AuthenticatedPrincipal` resuelto alcanzan el handler de dominio
    - **Validates: Requirements 18.1**
  
  - [ ]* 5.6 Property test: universalidad de la auditoría de seguridad
    - **Property 18: Universalidad de la auditoría de seguridad**
    - Generar peticiones aleatorias con resultados de auth (éxito/fallo, ambas etapas); verificar que se emite exactamente un evento de auditoría con `correlationId`, `userId` (si conocido), resultado y motivo
    - **Validates: Requirements 18.5, 18.6**

- [ ] 6. Paquete `wspr` y función unificada de autorización
  - [ ] 6.1 Definir la fachada pública del paquete `wspr`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.wspr`
    - Contratos: `Workspace`, `Project`, `Invitation`, `InvitationStatus`, `AccessDecision`, `AccessReason`
    - Consultas expuestas: `isProjectOwner`, `hasAcceptedInvitation`, `canAccessProject`, `getProject`, `listProjectsOwnedBy`, `listProjectsAccessibleTo`
    - Comandos expuestos: `createProject`, `deleteProject`, `inviteClient`, `acceptInvitation`, `revokeInvitation`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 11.1, 11.2, 11.5_
  
  - [ ] 6.2 Implementar el paquete `wspr`: dominio, repositorio y máquina de estados de `Invitation`
    - Entidades JPA para `Workspace`, `Project`, `Invitation` con Flyway (clases internas del paquete `wspr`)
    - Máquina de estados `Pending → Accepted | Revoked | Expired`; sin re-aceptación de `Revoked`
    - Invariantes estructurales: `Project.workspaceId` NOT NULL y FK válida, `Project.ownerAdminId` NOT NULL y con `role = PROJECT_ADMIN`
    - _Requirements: 2.2, 2.3, 3.1, 3.3, 3.5, 11.5_
  
  - [ ] 6.3 Implementar `canAccessProject` como bean público de la fachada `wspr` (doble llave rol + relación)
    - Orquesta `iam` + `wspr` inyectando la fachada de `iam`
    - Evaluar `PROJECT_ADMIN + ownership` **o** `CLIENT + accepted invitation + not blocked`
    - Retornar `AccessDecision` con `granted` + `reason` clasificada
    - Corto-circuito y ordenamiento explícito de razones de denegación según el diseño
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

- [ ] 7. Filtro global de autorización (fail-closed)
  - [ ] 7.1 Implementar filtro de autorización con corto-circuito en DENY
    - Reside en el paquete `security` bajo `io.github.bsidedevs.api_review.security`, invocando la fachada de `wspr` (`canAccessProject`)
    - Aplicado a toda operación de dominio (lectura, escritura, consulta) sin distinguir entre exposición y modificación
    - En cuanto un control retorna `DENY`, no se evalúan los siguientes
    - Fail-closed: si la decisión no se computa (error, ruta olvidada, configuración), la petición se deniega con 403 (o 404 si evita revelar existencia)
    - Registrar denegaciones en log de seguridad aunque el registro falle por razones técnicas
    - _Requirements: 9.6, 11.4, 18.2, 18.5, 18.7_
  
  - [ ]* 7.2 Property test: fail-closed (la denegación siempre gana)
    - **Property 9: Autorización fail-closed (la denegación siempre gana)**
    - Generar operaciones aleatorias con listas aleatorias de controles concurrentes; verificar que si al menos uno retorna `DENY`, la decisión final es `DENY` con independencia del éxito o fracaso del registro auxiliar
    - **Validates: Requirements 9.6, 11.4, 18.7**
  
  - [ ]* 7.3 Property test: universalidad de la autorización en toda petición
    - **Property 16: Universalidad de la autorización**
    - Generar rutas de dominio aleatorias (incluyendo rutas sin handler); verificar que siempre se computa una decisión antes del handler y, si no se computa, la respuesta es `DENY`
    - **Validates: Requirements 18.2, 18.7**

- [ ] 8. Checkpoint - autenticación y autorización fundacionales
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Núcleo del dominio: paquete `rs` (Review Session) y máquina de estados
  - [ ] 9.1 Definir la fachada pública del paquete `rs`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.rs`
    - Contratos: `ReviewSessionState`, `PersistenceStatus`, `ReviewSession`
    - Consultas expuestas: `getReviewSession`, `listActiveSessions`, `listAllSessions`, `currentState`
    - Comandos expuestos: `createSession`, `startRecording`, `completeRecording`, `reopen`, `archive`, `delete`
    - _Requirements: 4.4, 4.5, 4.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1_
  
  - [ ] 9.2 Implementar máquina de estados y matriz de transiciones válidas
    - Estado inicial `DRAFT`; `DELETED` absorbente
    - Toda transición no válida retorna error sin modificar estado
    - `RECORDING → COMPLETED` requiere `persistenceStatus = OK`
    - `reopen`, `archive` y `delete` exigen `PROJECT_ADMIN` propietario verificado (via fachada de `wspr`)
    - _Requirements: 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.11_
  
  - [ ] 9.3 Implementar el paquete `rs`: repositorio JPA y migraciones Flyway
    - Tabla `review_session` con columna `state` restringida al enum y `persistence_status` (clases internas del paquete `rs`)
    - Asociación inmutable a `Project` (FK NOT NULL, sin `UPDATE` de `project_id`)
    - _Requirements: 7.1, 8.1, 10.4_
  
  - [ ] 9.4 Implementar tracking de `persistenceStatus` y bloqueo aislado por sesión
    - Marcar `persistenceStatus = FAILED` cuando la persistencia de artefactos falla al finalizar la captura
    - Operaciones subsiguientes sobre esa sesión responden `409` con `reason: PERSISTENCE_PENDING`
    - Ninguna otra sesión resulta afectada
    - Definir job de reconciliación (esqueleto) que reintenta la persistencia y libera la sesión al éxito
    - _Requirements: 10.2_
  
  - [ ]* 9.5 Property test: buena-formación del estado
    - **Property 3: Buena-formación del estado de la Review Session**
    - Generar sesiones aleatorias y secuencias de comandos; verificar que `state` siempre pertenece a `{DRAFT, RECORDING, COMPLETED, REOPENED, ARCHIVED, DELETED}` y nunca es `NULL` ni admite dos valores simultáneos
    - **Validates: Requirements 8.1**
  
  - [ ]* 9.6 Property test: estado inicial DRAFT
    - **Property 4: Estado inicial DRAFT**
    - Para todo usuario autorizado y todo Proyecto, tras `createSession` exitoso, la sesión resultante tiene `state = DRAFT`
    - **Validates: Requirements 8.2**
  
  - [ ]* 9.7 Property test: validez de las transiciones de estado
    - **Property 5: Validez de las transiciones de estado**
    - Generar `(S, comando, actor)` aleatorios sobre la matriz completa (incluyendo intentos desde `DELETED`); verificar que la transición se aplica si y sólo si la terna pertenece al conjunto válido y que en caso contrario el estado permanece en `S` con error de transición inválida
    - **Validates: Requirements 4.5, 4.6, 7.4, 7.5, 7.6, 8.3, 8.4, 8.5, 8.6, 8.7, 8.11**
  
  - [ ]* 9.8 Property test: operaciones habilitadas por estado
    - **Property 6: Operaciones habilitadas por estado**
    - Para cada par `(estado, categoría de operación)`, verificar que la operación se acepta si y sólo si el par está habilitado en la tabla de operaciones-por-estado
    - **Validates: Requirements 8.8, 8.9, 8.10, 8.11**

- [ ] 10. Paquete `art`: fachada + agregado raíz de artefactos
  - [ ] 10.1 Definir la fachada pública del paquete `art`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.art`
    - Contratos: `ArtifactKind` (taxonomía completa), `Artifact` con `authorUserId`, `createdAt`, `payloadRef`, `metadata`
    - Consultas expuestas: `listArtifacts`, `getArtifact`
    - Captura expuesta: `captureArtifact`
    - Anotaciones expuestas: `addComment`, `replyToComment`, `addTextNote`, `addVoiceNote`
    - Modificación/eliminación de anotaciones propias: `updateOwnAnnotation`, `deleteOwnAnnotation`
    - _Requirements: 1.2, 9.1, 9.2, 9.3, 9.4, 9.7, 9.8, 10.5_
  
  - [ ] 10.2 Implementar el paquete `art` con autoría y timestamp inmutables
    - Persistir `Artifact` con FK NOT NULL a `review_session` y a `user` (autor) (clases internas del paquete `art`)
    - `author_user_id` y `created_at` son inmutables tras la creación (columnas `UPDATE`-protegidas por trigger o repositorio)
    - Reglas de familia: `CaptureArtifact` sólo en `RECORDING`; `AnnotationArtifact` en `RECORDING | COMPLETED | REOPENED` (consultado vía fachada de `rs`)
    - Modificación/eliminación de `COMMENT`, `TEXT_NOTE`, `VOICE_NOTE` restringida al `authorUserId`
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
    - **Validates: Requirements 10.1, 10.3, 10.4**
  
  - [ ]* 10.8 Property test: atomicidad y aislamiento del fallo de persistencia
    - **Property 14: Atomicidad y aislamiento de la persistencia**
    - Simular fallos de persistencia aleatorios en la transición `RECORDING → COMPLETED`; verificar que la sesión afectada queda bloqueada (`persistenceStatus = FAILED`), los artefactos fallidos no establecen asociación y ninguna otra sesión resulta afectada
    - **Validates: Requirements 10.2, 10.5**

- [ ] 11. Paquete `notif`: fachada + mapeo determinístico
  - [ ] 11.1 Definir la fachada pública del paquete `notif`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.notif`
    - Contratos: `NotificationKind`, `DeliveryStatus`, `Notification`
    - Puntos de entrada expuestos: `notifyCommentAdded`, `notifyCommentReplied`, `notifySessionCompleted`, `notifyAiProcessingCompleted`
    - Consultas expuestas: `listMyNotifications`, `markAsConsumed`
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [ ] 11.2 Implementar el paquete `notif`: mapeo evento → destinatarios con regla de consolidación
    - Consumir las fachadas de `iam`, `wspr`, `rs`, `art` para resolver destinatarios (clases internas del paquete `notif`)
    - `commentAdded` → notifica al Administrador de Proyecto propietario
    - `commentReplied` → autor original + Administrador propietario; si coinciden, **una única** notificación consolidada
    - `sessionCompleted` → todos los participantes autorizados del Proyecto
    - `aiProcessingCompleted` → Administrador de Proyecto propietario
    - _Requirements: 12.1, 12.2, 12.3, 12.4_
  
  - [ ] 11.3 Implementar política de reintento del registro (hasta 3 intentos)
    - 1 intento original + 2 reintentos con backoff
    - Tras 3 fallos, marcar `deliveryStatus = FAILED` (visible en Backoffice para diagnóstico)
    - _Requirements: 12.5, 12.6_
  
  - [ ]* 11.4 Property test: determinismo del mapeo evento → destinatarios
    - **Property 19: Determinismo del mapeo evento → destinatarios de notificación**
    - Generar eventos aleatorios (comentario, respuesta, sesión completada, IA finalizada) con actores/propietarios aleatorios; verificar el conjunto exacto de notificaciones según `Modelo 6` y la consolidación 1-única para respuestas donde autor original = propietario
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4**
  
  - [ ]* 11.5 Unit tests del reintento del registro
    - Éxito en el 1º, 2º y 3º intento; fallo tras 3 → `deliveryStatus = FAILED`
    - _Requirements: 12.6_

- [ ] 12. Paquete `boff`: fachada + consultas administrativas
  - [ ] 12.1 Definir la fachada pública del paquete `boff`
    - Interfaces con anotaciones Spring expuestas como beans desde `io.github.bsidedevs.api_review.boff`
    - Contratos: `PlatformMetrics`
    - Consultas expuestas: `listProjectAdmins`, `listClients`, `getMetrics`
    - Cerradas al rol `PLATFORM_ADMIN` (validado en el filtro de autorización)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [ ] 12.2 Implementar el paquete `boff`: consultas agregadas sin exposición de contenido
    - Consumir las fachadas de `iam`, `wspr`, `rs` (clases internas del paquete `boff`)
    - Contadores (totales por rol, workspaces, proyectos, sesiones por estado)
    - Listado paginado de administradores y clientes registrados
    - Ninguna operación devuelve payload de artefactos de Review Sessions
    - Listados vacíos responden colección vacía (no error)
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 11.3_
  
  - [ ]* 12.3 Unit tests del Backoffice
    - Listados vacíos responden `200` con colección vacía
    - Cualquier intento de acceso a contenido de Review Session desde Backoffice es rechazado
    - _Requirements: 6.3, 6.5, 6.6, 11.3_

- [ ] 13. Checkpoint - paquetes de dominio fundacionales
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Capa REST + publicación de OpenAPI (paquete `rest`)
  - [ ] 14.1 Configurar capa REST base con Spring MVC
    - Reside en `io.github.bsidedevs.api_review.rest`; base path `/api/v1`
    - Traducción HTTP → llamadas a las fachadas públicas de dominio (sin acceso directo a clases internas)
    - Cadena de filtros: `correlationFilter` → `authenticationFilter` → `authorizationFilter` → handler
    - Serialización de errores como `Problem+JSON`
    - Códigos por categoría (`200/201/204/400/401/403/404/409/422/5xx`) según diseño
    - _Requirements: 13.1, 17.5, 18.1, 18.2_
  
  - [ ] 14.2 Configurar generación automática de OpenAPI (springdoc-openapi)
    - Anotar contratos de fachada y controladores para reflejar todas las operaciones expuestas
    - La generación de la especificación no interrumpe el tráfico HTTP en curso
    - _Requirements: 13.2, 13.3, 13.4, 13.5, 13.6_
  
  - [ ] 14.3 Exponer la especificación OpenAPI en endpoints HTTP
    - `GET /api/v1/openapi.json`, `GET /api/v1/openapi.yaml`
    - `GET /api/v1/docs` (Swagger UI opcional)
    - Servir la especificación previa aunque falle la regeneración
    - _Requirements: 13.4, 13.5, 13.6, 13.7_
  
  - [ ] 14.4 Configurar terminación TLS en el proxy y flags de seguridad
    - Configuración de reverse proxy en `infra/docker/` para TLS
    - Cookies de sesión `Secure` + `HttpOnly` + `SameSite`
    - _Requirements: 18.3_
  
  - [ ]* 14.5 Integration test: disponibilidad del endpoint OpenAPI ante fallo de regeneración
    - Simular fallo de regeneración y verificar que la API sigue operativa y que `/openapi.json` sirve la especificación previa
    - _Requirements: 13.4, 13.5, 13.6_

- [ ] 15. Frontend SPA: scaffolding, i18n y responsive
  - [ ] 15.1 Bootstrap del cliente REST y del layout base
    - Configurar cliente HTTP TypeScript que consume `/api/v1` con `X-Correlation-Id` opcional en request
    - Layout base con enrutado inicial y páginas placeholder por rol (`ProjectAdmin`, `Client`, `PlatformAdmin`)
    - _Requirements: 13.7, 15.1, 17.5_
  
  - [ ] 15.2 Implementar cargador de bundles i18n con política todo-o-nada
    - Bundles por locale en `frontend/src/i18n/{locale}.json`
    - Resolver locale efectivo una única vez por render
    - Si el locale seleccionado tiene todas las claves requeridas → renderizar entero en ese locale (aunque no sea "oficialmente soportado")
    - Si cualquier clave requerida falta → renderizar entero en el locale por defecto (sin mezclar idiomas)
    - _Requirements: 14.1, 14.2, 14.3_
  
  - [ ] 15.3 Implementar layout responsive (desktop + mobile)
    - Grid responsive con breakpoints
    - Vistas de consulta y comentario disponibles en resoluciones móviles
    - _Requirements: 15.1, 15.2_
  
  - [ ]* 15.4 Property test: i18n todo-o-nada por locale (fast-check)
    - **Property 20: Render de i18n todo-o-nada por locale**
    - Generar bundles aleatorios con claves faltantes y conjuntos requeridos aleatorios; verificar que el render efectivo es enteramente en el locale seleccionado si y sólo si todas las claves están presentes; en caso contrario, enteramente en el locale por defecto
    - **Validates: Requirements 14.3**
  
  - [ ]* 15.5 Component tests de breakpoints responsive
    - Verificar renderizado de vistas de consulta y comentario en viewports móvil y desktop
    - _Requirements: 15.1, 15.2_

- [ ] 16. Extensión Autorizada: scaffolding
  - [ ] 16.1 Bootstrap del paquete de extensión de navegador
    - Manifest WebExtension (MV3) con permisos mínimos necesarios
    - Estructura de background/service worker + content script + popup UI en TypeScript
    - _Requirements: 5.1_
  
  - [ ] 16.2 Implementar cliente REST + reutilización del mecanismo de autenticación
    - Cliente HTTP compartido con el frontend (mismo endpoint `/api/v1`, misma sesión / token)
    - _Requirements: 5.1, 18.1_
  
  - [ ]* 16.3 Unit tests del flujo de autenticación de la extensión
    - Verificar que la extensión autentica con el mismo mecanismo que la SPA
    - _Requirements: 5.1, 18.1_

- [ ] 17. Test fixtures y tests de arquitectura
  - [ ] 17.1 Implementar el paquete `test-fixtures` bajo `src/test/java`
    - Ubicado en `backend/src/test/java/io/github/bsidedevs/api_review/testfixtures/`, compartido por todos los tests del monolito (no es un módulo Maven separado)
    - Añadir dependencia `net.jqwik:jqwik` (scope `test`) en `backend/pom.xml`
    - Generadores `arbUser(role)`, `arbProject(ownerAdminId)`, `arbReviewSessionInState(state)`, `arbAccessScenario`, `arbArtifactKind`
    - Generadores adaptados a jqwik (backend); los generadores equivalentes con fast-check viven en el proyecto `frontend/` (creado en 1.2)
    - _Requirements: 16.3_
  
  - [ ] 17.2 Añadir reglas ArchUnit sobre el grafo de dependencias de paquetes
    - Los paquetes de dominio no importan clases marcadas como internas de otro dominio (por convención: subpaquetes `.internal.*` no son accesibles desde fuera del dominio propietario)
    - Sólo se importan clases del paquete raíz del dominio proveedor (donde vive la fachada Spring bean)
    - El grafo de dependencias entre dominios es un DAG conforme al diseño: `iam ← wspr ← rs ← art`, `notif → {rs, art, wspr, iam}`, `boff → {iam, wspr, rs}`
    - El paquete `shared` es importable por cualquier dominio; no depende de ningún dominio
    - La capa `rest` no llama directamente a clases marcadas como internas de dominio; sólo a fachadas
    - _Requirements: 19.1, 19.2_
  
  - [ ]* 17.3 Smoke test de integración con Testcontainers + PostgreSQL + Flyway
    - Levantar contexto Spring Boot mínimo contra PostgreSQL 16 en Testcontainers
    - Ejecutar migraciones Flyway y verificar arranque limpio
    - _Requirements: 16.1, 16.3, 19.3_

- [ ] 18. Architecture Decision Records iniciales
  - [ ] 18.1 Crear estructura `docs/adr/` con plantilla estándar
    - Plantilla con secciones: Contexto, Alternativas consideradas, Decisión, Consecuencias, Referencias
    - _Requirements: 20.1, 20.2_
  
  - [ ] 18.2 ADR-001: Monolito Spring Boot con paquetes por dominio
    - Contexto: se optó por un monolito Spring Boot simple con paquetes por dominio dentro de un único módulo Maven para minimizar la complejidad inicial del MVP
    - Alternativas consideradas: (a) microservicios desde el inicio, (b) monolito modular con módulos Maven separados `*-api`/`*-impl`, (c) monolito plano sin fronteras internas
    - Decisión: monolito de un único módulo Maven con paquetes por dominio y fronteras internas materializadas por convención (paquetes `internal.*` privados al dominio) + ArchUnit
    - Consecuencias: las fronteras entre dominios se preservan mediante disciplina de código y reglas ArchUnit, no mediante fronteras de build; la extracción a servicios independientes queda fuera del alcance MVP (Requirement 19 fue relajado)
    - _Requirements: 19.1, 19.2, 20.1_
  
  - [ ] 18.3 ADR-002: Elección de bibliotecas PBT (jqwik + fast-check)
    - Justificar la elección y sus alternativas
    - _Requirements: 20.1_
  
  - [ ] 18.4 ADR-003: Algoritmo de hashing de credenciales
    - Decidir entre bcrypt y argon2; documentar parámetros
    - _Requirements: 18.4, 20.1_
  
  - [ ] 18.5 ADR-004: Estrategia inicial de almacenamiento de payloads
    - `bytea` con TOAST en primera iteración MVP vs. object storage S3-compatible
    - _Requirements: 20.1_
  
  - [ ] 18.6 ADR-005: Locale por defecto y política de fallback i18n
    - Documentar `default_locale = "es"` (ajustable) y la política todo-o-nada
    - _Requirements: 14.3, 20.1_
  
  - [ ] 18.7 ADR-006: Esquema de identificadores (UUID v7)
    - Justificar UUID v7 (ordenable + único) frente a UUID v4 y snowflake
    - _Requirements: 20.1_

- [ ] 19. Checkpoint final - fundación lista
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Las tareas marcadas con `*` son **opcionales** (tests unitarios, tests de propiedades e integración) y pueden omitirse en un MVP rápido; sin embargo, según Requirement 16, toda funcionalidad entregada dentro del alcance MVP requiere pruebas escritas como precondición a considerarse terminada.
- Cada tarea referencia sub-cláusulas concretas de los criterios de aceptación en `_Requirements: X.Y_`, garantizando trazabilidad requisito → tarea.
- Los **20 property tests** están agrupados junto al código que validan (no como suite separada), siguiendo la recomendación de `Testing Strategy` de detectar errores lo antes posible.
- Todos los property tests usan como mínimo **100 iteraciones** y llevan la etiqueta `Feature: reviews-platform-foundation, Property N` en el nombre del test, según el diseño.
- Los checkpoints (tareas 8, 13, 19) son puntos naturales de validación incremental; cada uno recuerda ejecutar la batería completa y consultar al usuario ante dudas.
- Este spec es la **fundación**: la subestructura interna de cada paquete de dominio (subpaquetes `controllers/`, `services/`, `repositories/`, entidades JPA concretas, migraciones Flyway detalladas, jobs de reconciliación completos, etc.) se desarrollará en specs posteriores tomando este documento como marco de referencia. Aquí sólo se materializa la fachada pública de cada dominio y las clases internas mínimas necesarias para satisfacer las propiedades declaradas.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "18.1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "18.2", "18.3", "18.4", "18.5", "18.6", "18.7"] },
    { "id": 2, "tasks": ["1.3", "2.3", "3.1", "3.2", "3.3", "3.4", "4.1", "5.1", "6.1", "9.1", "10.1", "11.1", "12.1", "15.1", "16.1"] },
    { "id": 3, "tasks": ["3.6", "4.2", "5.2", "5.3", "6.2", "9.2", "10.2", "11.2", "12.2", "15.2", "15.3", "16.2", "17.1"] },
    { "id": 4, "tasks": ["3.5", "4.3", "5.4", "6.3", "6.4", "9.3", "9.5", "9.6", "10.3", "10.4", "10.5", "11.3", "12.3", "15.4", "15.5", "16.3"] },
    { "id": 5, "tasks": ["5.5", "5.6", "6.5", "6.6", "7.1", "9.4", "9.7", "9.8", "10.6", "11.4", "11.5", "17.2"] },
    { "id": 6, "tasks": ["7.2", "7.3", "10.7", "10.8", "14.1", "14.4"] },
    { "id": 7, "tasks": ["14.2", "14.3", "17.3"] },
    { "id": 8, "tasks": ["14.5"] }
  ]
}
```
