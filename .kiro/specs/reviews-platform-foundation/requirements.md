# Requirements Document

## Introduction

Reviews es una plataforma SaaS orientada a freelancers, agencias y equipos de desarrollo que necesitan revisar aplicaciones web de forma colaborativa. El sistema centraliza el proceso completo de revisión de una aplicación web en una única unidad de trabajo denominada Review Session, eliminando la dispersión de capturas de pantalla, documentos, correos y mensajes.

Durante una Review Session el usuario navega normalmente sobre la aplicación web bajo revisión mientras la Plataforma captura automáticamente la información relevante (grabación rrweb, snapshots del DOM, timeline de eventos, metadatos del navegador, anotaciones Excalidraw, comentarios, notas de texto, notas de voz y artefactos generados por IA). Al finalizar la captura, la sesión queda almacenada de forma persistente y puede reabrirse posteriormente para continuar agregando comentarios y observaciones.

Este documento constituye el spec de **fundación** de la plataforma. Su objetivo es capturar la visión, el modelo de dominio, los actores, las invariantes, los estados del ciclo de vida de una Review Session, y los requisitos no funcionales transversales. Los detalles de cada módulo (modelos de datos concretos, endpoints, pantallas, contratos internos) se desarrollarán en specs posteriores, módulo por módulo, tomando este documento como marco de referencia.

## Glossary

- **Plataforma**: sistema SaaS Reviews en su conjunto (backend, frontend, extensión de navegador autorizada, servicios de soporte).
- **Equipo de la Plataforma**: conjunto de personas responsables del desarrollo y la evolución de la Plataforma; es el sujeto de los requisitos de proceso (por ejemplo, documentación de decisiones arquitectónicas).
- **Workspace**: espacio principal donde un Administrador de Proyecto gestiona sus Proyectos y Review Sessions.
- **Proyecto**: agrupación lógica que contiene las Review Sessions asociadas a una aplicación o sitio web bajo revisión.
- **Proyecto Bloqueado**: Proyecto cuyo indicador de bloqueo se encuentra activo. Mientras el indicador está activo, la Plataforma deniega el acceso de los Clientes al contenido del Proyecto. Los mecanismos de activación y desactivación del indicador se definen en specs posteriores.
- **Review Session**: unidad principal de trabajo del dominio; contiene todos los artefactos generados durante una revisión completa.
- **Metadatos de la Review Session**: atributos descriptivos de una Review Session (nombre, descripción, dirección de la aplicación bajo revisión) que no forman parte de sus artefactos.
- **Artefacto**: cualquier pieza de información producida y persistida dentro de una Review Session (grabación rrweb, snapshot, entrada del timeline, escena Excalidraw, comentario, nota de texto, nota de voz, transcripción, resumen o resultado de IA, metadato de navegador).
- **Comentario**: observación textual realizada por un usuario autorizado sobre una Review Session.
- **Nota de Texto**: anotación textual libre asociada a una Review Session o a un instante del timeline.
- **Nota de Voz**: grabación de audio asociada a una Review Session; puede tener transcripción asociada.
- **Snapshot**: captura del estado del DOM en un instante determinado durante la Review Session.
- **Timeline**: secuencia cronológica de los eventos registrados durante una Review Session.
- **Grabación rrweb**: registro de la interacción del usuario con la aplicación web bajo revisión, capturado mediante la biblioteca rrweb.
- **Escena Excalidraw**: conjunto de anotaciones gráficas creadas por un usuario sobre la Review Session utilizando Excalidraw.
- **Resumen IA**: artefacto generado por un componente de inteligencia artificial a partir del contenido de una Review Session.
- **Administrador de Proyecto**: usuario propietario de uno o más Proyectos; representa a un freelancer, agencia o equipo de desarrollo.
- **Cliente**: usuario invitado por un Administrador de Proyecto a colaborar en uno o más Proyectos.
- **Administrador de Plataforma**: usuario del Backoffice que administra el SaaS; no participa en revisiones.
- **Participante Autorizado**: usuario con acceso vigente al contenido de un Proyecto, es decir, el Administrador de Proyecto propietario del Proyecto o un Cliente con Invitación aceptada sobre ese Proyecto.
- **Backoffice**: interfaz administrativa de la Plataforma reservada al Administrador de Plataforma.
- **Extensión Autorizada**: extensión de navegador provista por la Plataforma que permite al Cliente iniciar y participar en Review Sessions.
- **Invitación**: mecanismo mediante el cual un Administrador de Proyecto autoriza a un Cliente a acceder a un Proyecto específico. Toma uno de los estados `Pendiente`, `Aceptada`, `Revocada` o `Expirada`.
- **Listado Activo**: listado de las Review Sessions de un Proyecto que excluye las Review Sessions en estado `Archived` y en estado `Deleted`.
- **Listado Histórico**: listado de las Review Sessions de un Proyecto que incluye las Review Sessions en estado `Archived`.
- **API REST**: interfaz HTTP expuesta por la Plataforma que sigue el estilo arquitectónico REST.
- **OpenAPI**: especificación abierta utilizada para documentar la API REST de la Plataforma.
- **Especificación OpenAPI Vigente**: última especificación OpenAPI publicada satisfactoriamente por la Plataforma.
- **ADR (Architecture Decision Record)**: documento que registra una decisión arquitectónica relevante junto con su contexto, alternativas y consecuencias.
- **Estado de Review Session**: valor del ciclo de vida de una Review Session; toma uno de los valores `Draft`, `Recording`, `Completed`, `Reopened`, `Archived`, `Deleted`.
- **Identificador de Correlación**: identificador único asociado a una operación que permite vincular entre sí todos los registros de observabilidad emitidos durante su ejecución.
- **Logs de Seguridad**: conjunto de registros estructurados dedicados a eventos de autenticación, autorización e intentos de acceso no autorizado.
- **Historial de Notificaciones**: registro de las notificaciones generadas para un usuario destinatario, consultable por ese usuario dentro de la Plataforma.
- **Estado de Entrega**: resultado del registro de una notificación en el Historial de Notificaciones; toma uno de los valores `Pendiente`, `Registrada` o `Fallida`.
- **Traducción Requerida**: entrada de texto de internacionalización necesaria para presentar la interfaz de la Plataforma en un idioma determinado.
- **Idioma Completo**: idioma para el cual todas las Traducciones Requeridas están disponibles en los recursos de internacionalización.
- **Idioma por Defecto**: idioma en el que la Plataforma presenta la interfaz cuando el idioma seleccionado no es un Idioma Completo.
- **Pipeline de Integración Continua**: automatización que ejecuta la compilación y las pruebas del proyecto ante cada cambio del código fuente.

## Requirements

### Requirement 1: Visión y unidad de trabajo principal

**User Story:** Como Administrador de Proyecto o Cliente, quiero que toda la información de una revisión de aplicación web quede centralizada en una única Review Session, para no depender de capturas de pantalla, correos ni herramientas externas dispersas.

#### Acceptance Criteria

1. THE Plataforma SHALL modelar la Review Session como la unidad principal de trabajo del dominio, agrupando todos los artefactos generados durante una revisión.
2. THE Plataforma SHALL asociar todos los artefactos producidos durante una revisión a la misma Review Session.
3. WHEN un usuario solicita acceder a una Review Session persistida, THE Plataforma SHALL verificar la autorización de ese usuario sobre la Review Session antes de exponer los artefactos asociados.
4. WHEN la verificación de autorización de un usuario sobre una Review Session persistida resulta satisfactoria, THE Plataforma SHALL exponer únicamente los artefactos asociados a esa Review Session.
5. IF una Review Session persistida no dispone de artefactos asociados, THEN THE Plataforma SHALL presentar un estado vacío que indique la ausencia de artefactos capturados.
6. THE Plataforma SHALL mantener un único contexto compartido por Review Session para todos los Participantes Autorizados del Proyecto asociado.

### Requirement 2: Jerarquía del dominio Workspace, Proyecto y Review Session

**User Story:** Como Administrador de Proyecto, quiero que la información se organice según la jerarquía Workspace → Proyecto → Review Session, para poder gestionar múltiples clientes y aplicaciones sin mezclar contenido.

#### Acceptance Criteria

1. THE Plataforma SHALL organizar la información según la jerarquía Workspace, Proyecto y Review Session, donde un Workspace contiene Proyectos y un Proyecto contiene Review Sessions.
2. THE Plataforma SHALL asociar cada Proyecto a un único Workspace.
3. THE Plataforma SHALL asociar cada Review Session a un único Proyecto.
4. THE Plataforma SHALL permitir que un Workspace contenga cero o más Proyectos.
5. THE Plataforma SHALL permitir que un Proyecto contenga cero o más Review Sessions.

### Requirement 3: Invariantes del Proyecto

**User Story:** Como Administrador de Proyecto, quiero reglas claras de propiedad y colaboración sobre los Proyectos, para que quede definido quién puede administrarlos y quién puede colaborar en ellos.

#### Acceptance Criteria

1. THE Plataforma SHALL asignar cada Proyecto a un único Administrador de Proyecto como propietario.
2. THE Plataforma SHALL permitir que un mismo Administrador de Proyecto sea propietario de múltiples Proyectos.
3. THE Plataforma SHALL permitir asociar un mismo Cliente a uno o varios Proyectos mediante Invitaciones independientes.
4. WHERE un Cliente no dispone de una Invitación en estado `Aceptada` sobre un Proyecto, THE Plataforma SHALL denegar el acceso de ese Cliente al contenido de ese Proyecto.
5. WHILE una Invitación de un Cliente sobre un Proyecto permanece en estado `Pendiente`, THE Plataforma SHALL denegar el acceso de ese Cliente al contenido de ese Proyecto.
6. WHEN una Invitación de un Cliente sobre un Proyecto que no se encuentra en condición de Proyecto Bloqueado transiciona al estado `Aceptada`, THE Plataforma SHALL otorgar a ese Cliente acceso al contenido de ese Proyecto.

### Requirement 4: Rol Administrador de Proyecto

**User Story:** Como Administrador de Proyecto, quiero disponer de un conjunto acotado de operaciones sobre mis Proyectos y sus Review Sessions, para gestionar de forma controlada el ciclo de revisión con mis Clientes.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Administrador de Proyecto crear Proyectos sin exigir la propiedad previa de ningún otro Proyecto.
2. THE Plataforma SHALL permitir al Administrador de Proyecto editar y eliminar únicamente los Proyectos de los que sea propietario.
3. THE Plataforma SHALL permitir al Administrador de Proyecto invitar Clientes a los Proyectos de los que sea propietario.
4. THE Plataforma SHALL permitir al Administrador de Proyecto iniciar Review Sessions sobre los Proyectos de los que sea propietario.
5. THE Plataforma SHALL permitir al Administrador de Proyecto cerrar las Review Sessions de los Proyectos de los que sea propietario.
6. THE Plataforma SHALL permitir al Administrador de Proyecto reabrir las Review Sessions en estado `Completed` de los Proyectos de los que sea propietario.
7. THE Plataforma SHALL permitir al Administrador de Proyecto visualizar todas las Review Sessions de los Proyectos de los que sea propietario.
8. THE Plataforma SHALL permitir al Administrador de Proyecto agregar comentarios y responder comentarios sobre las Review Sessions de los Proyectos de los que sea propietario.
9. IF un Administrador de Proyecto intenta acceder a un Proyecto del que no es propietario, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado en los Logs de Seguridad.
10. WHEN un Administrador de Proyecto crea un Proyecto, THE Plataforma SHALL asignar a ese usuario como propietario del Proyecto creado.
11. THE Plataforma SHALL permitir al Administrador de Proyecto archivar las Review Sessions en estado `Completed` de los Proyectos de los que sea propietario.

### Requirement 5: Rol Cliente

**User Story:** Como Cliente invitado a un Proyecto, quiero poder participar en las Review Sessions de ese Proyecto tanto desde la aplicación web como desde una Extensión Autorizada, para colaborar sin necesidad de acceso administrativo.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Cliente acceder a la Plataforma mediante la aplicación web y mediante la Extensión Autorizada.
2. WHERE un Cliente dispone de una Invitación en estado `Aceptada` sobre un Proyecto que no se encuentra en condición de Proyecto Bloqueado, THE Plataforma SHALL permitir a ese Cliente iniciar Review Sessions sobre ese Proyecto.
3. WHERE un Cliente dispone de una Invitación en estado `Aceptada` sobre un Proyecto que no se encuentra en condición de Proyecto Bloqueado, THE Plataforma SHALL permitir a ese Cliente visualizar las Review Sessions de ese Proyecto.
4. WHERE un Cliente dispone de una Invitación en estado `Aceptada` sobre un Proyecto que no se encuentra en condición de Proyecto Bloqueado, THE Plataforma SHALL permitir a ese Cliente agregar y responder comentarios sobre las Review Sessions de ese Proyecto.
5. WHEN un Cliente solicita acceder al contenido de un Proyecto, THE Plataforma SHALL verificar que ese Cliente dispone de una Invitación en estado `Aceptada` sobre ese Proyecto antes de servir la operación.
6. IF un Cliente intenta acceder a un Proyecto sobre el que no dispone de una Invitación en estado `Aceptada`, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado en los Logs de Seguridad.
7. WHILE un Proyecto se encuentra en condición de Proyecto Bloqueado, THE Plataforma SHALL denegar el acceso de los Clientes al contenido de ese Proyecto, incluido el acceso de los Clientes que disponen de una Invitación en estado `Aceptada` sobre ese Proyecto.

### Requirement 6: Rol Administrador de Plataforma (Backoffice)

**User Story:** Como Administrador de Plataforma, quiero un Backoffice separado de la operación de revisiones, para administrar el SaaS y consultar métricas sin interferir con el contenido de los Proyectos ni de las Review Sessions.

#### Acceptance Criteria

1. THE Plataforma SHALL exponer un Backoffice accesible únicamente al Administrador de Plataforma.
2. THE Plataforma SHALL permitir al Administrador de Plataforma visualizar los Administradores de Proyecto registrados y los Clientes registrados.
3. IF al momento de una consulta del Administrador de Plataforma no existen Administradores de Proyecto registrados o no existen Clientes registrados, THEN THE Plataforma SHALL presentar en el Backoffice un listado vacío.
4. THE Plataforma SHALL permitir al Administrador de Plataforma consultar métricas generales de operación de la Plataforma.
5. THE Plataforma SHALL impedir al Administrador de Plataforma participar en Review Sessions.
6. THE Plataforma SHALL impedir al Administrador de Plataforma modificar el contenido de Review Sessions.

### Requirement 7: Invariantes de la Review Session

**User Story:** Como usuario autorizado, quiero reglas de dominio claras sobre la Review Session, para tener certeza de a qué Proyecto pertenece, quién puede accederla y bajo qué condición de doble llave (rol y relación) se autoriza el acceso o las operaciones sobre su ciclo de vida.

#### Acceptance Criteria

1. THE Plataforma SHALL asociar cada Review Session a un único Proyecto durante toda su existencia, sin que esta asociación pueda modificarse después de la creación.
2. THE Plataforma SHALL tratar cada Review Session como un recurso compartido del Proyecto al que pertenece.
3. WHERE un usuario dispone del rol de Administrador de Proyecto y es el propietario verificado de un Proyecto, THE Plataforma SHALL permitir a ese usuario acceder a las Review Sessions de ese Proyecto.
4. THE Plataforma SHALL permitir que una Review Session sea abierta, cerrada, reabierta, archivada y eliminada durante su ciclo de vida, únicamente por los actores y desde los estados previstos en el ciclo de vida.
5. IF un usuario dispone del rol de Administrador de Proyecto y no es el propietario del Proyecto asociado a una Review Session, THEN THE Plataforma SHALL denegar a ese usuario el acceso a esa Review Session y registrar el intento de acceso no autorizado en los Logs de Seguridad.
6. IF un usuario es el propietario registrado de un Proyecto pero no dispone del rol de Administrador de Proyecto, THEN THE Plataforma SHALL denegar a ese usuario el acceso a las Review Sessions de ese Proyecto y registrar el intento de acceso no autorizado en los Logs de Seguridad.
7. THE Plataforma SHALL restringir la eliminación de una Review Session al Administrador de Proyecto propietario del Proyecto asociado.
8. IF un usuario que no es el Administrador de Proyecto propietario del Proyecto asociado intenta eliminar una Review Session, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado en los Logs de Seguridad.

### Requirement 8: Ciclo de vida y estados de la Review Session

**User Story:** Como Administrador de Proyecto o Cliente autorizado, quiero que la Review Session tenga un ciclo de vida explícito con estados bien definidos, para saber en todo momento qué operaciones están disponibles sobre ella.

#### Acceptance Criteria

1. THE Plataforma SHALL asignar a cada Review Session exactamente uno de los siguientes estados en cualquier instante: `Draft`, `Recording`, `Completed`, `Reopened`, `Archived`, `Deleted`.
2. WHEN una Review Session es creada, THE Plataforma SHALL inicializar su estado en `Draft`.
3. WHEN un Participante Autorizado solicita iniciar la captura de una Review Session cuyo estado actual es `Draft`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Recording`.
4. WHEN un Participante Autorizado solicita finalizar la captura de una Review Session cuyo estado actual es `Recording`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Completed`.
5. WHEN el Administrador de Proyecto propietario del Proyecto asociado solicita reabrir una Review Session cuyo estado actual es `Completed`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Reopened`.
6. WHEN el Administrador de Proyecto propietario del Proyecto asociado solicita archivar una Review Session cuyo estado actual es `Completed`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Archived`.
7. WHEN el Administrador de Proyecto propietario del Proyecto asociado solicita eliminar una Review Session cuyo estado actual es distinto de `Deleted`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Deleted`.
8. WHILE una Review Session se encuentra en estado `Recording`, THE Plataforma SHALL permitir la captura de artefactos asociados a esa Review Session.
9. WHILE una Review Session se encuentra en estado `Completed` o `Reopened`, THE Plataforma SHALL permitir la creación de comentarios, notas y anotaciones asociados a esa Review Session.
10. WHILE una Review Session se encuentra en estado `Draft`, `Recording`, `Completed` o `Reopened`, THE Plataforma SHALL incluir esa Review Session en el Listado Activo del Proyecto al que pertenece.
11. WHEN el estado de una Review Session transiciona a `Deleted`, THE Plataforma SHALL rechazar desde ese instante toda operación de modificación sobre esa Review Session y sobre sus artefactos asociados.
12. WHEN un Participante Autorizado solicita iniciar una nueva captura sobre una Review Session cuyo estado actual es `Reopened`, THE Plataforma SHALL transicionar el estado de esa Review Session a `Recording`.
13. IF un usuario solicita iniciar la captura de una Review Session cuyo estado actual es distinto de `Draft` y distinto de `Reopened`, THEN THE Plataforma SHALL rechazar la solicitud y conservar el estado actual de esa Review Session.
14. IF un usuario solicita finalizar la captura de una Review Session cuyo estado actual es distinto de `Recording`, THEN THE Plataforma SHALL rechazar la solicitud y conservar el estado actual de esa Review Session.
15. IF un usuario solicita reabrir una Review Session cuyo estado actual es distinto de `Completed`, THEN THE Plataforma SHALL rechazar la solicitud y conservar el estado actual de esa Review Session.
16. IF un usuario solicita archivar una Review Session cuyo estado actual es distinto de `Completed`, THEN THE Plataforma SHALL rechazar la solicitud y conservar el estado actual de esa Review Session.
17. IF un usuario que no es el Administrador de Proyecto propietario del Proyecto asociado solicita reabrir una Review Session, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado en los Logs de Seguridad.
18. IF un usuario que no es el Administrador de Proyecto propietario del Proyecto asociado solicita archivar una Review Session, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado en los Logs de Seguridad.
19. WHILE una Review Session se encuentra en estado `Draft`, THE Plataforma SHALL permitir a los Participantes Autorizados editar los Metadatos de la Review Session.
20. WHILE una Review Session se encuentra en estado `Draft`, THE Plataforma SHALL rechazar la creación de artefactos asociados a esa Review Session.
21. WHILE una Review Session se encuentra en estado `Recording`, THE Plataforma SHALL permitir la creación de comentarios, notas y anotaciones asociados a esa Review Session.
22. WHILE una Review Session se encuentra en estado `Completed` o `Reopened`, THE Plataforma SHALL rechazar la captura de nuevos eventos de la aplicación web bajo revisión para esa Review Session.
23. WHILE una Review Session se encuentra en estado `Archived`, THE Plataforma SHALL permitir a los Participantes Autorizados consultar los artefactos asociados a esa Review Session.
24. WHILE una Review Session se encuentra en estado `Archived`, THE Plataforma SHALL rechazar la creación, la modificación y la eliminación de artefactos asociados a esa Review Session.
25. WHILE una Review Session se encuentra en estado `Archived`, THE Plataforma SHALL excluir esa Review Session del Listado Activo del Proyecto al que pertenece e incluirla en el Listado Histórico de ese Proyecto.
26. WHILE una Review Session se encuentra en estado `Archived`, THE Plataforma SHALL rechazar toda solicitud de transición de estado sobre esa Review Session distinta de la eliminación.
27. WHILE una Review Session se encuentra en estado `Deleted`, THE Plataforma SHALL rechazar toda solicitud de transición de estado sobre esa Review Session.
28. WHEN el estado de una Review Session transiciona a `Deleted`, THE Plataforma SHALL excluir desde ese instante esa Review Session del Listado Activo y del Listado Histórico del Proyecto al que pertenece.

### Requirement 9: Autoría y edición de artefactos

**User Story:** Como usuario que produce artefactos dentro de una Review Session, quiero que mi autoría quede registrada de forma permanente y que solo yo pueda modificar o eliminar mis propios comentarios y notas, para preservar la trazabilidad y la integridad de la revisión.

#### Acceptance Criteria

1. WHEN un artefacto es creado dentro de una Review Session, THE Plataforma SHALL registrar al usuario autor de ese artefacto.
2. THE Plataforma SHALL conservar sin modificaciones el registro de autoría de cada artefacto durante toda la vida de la Review Session a la que pertenece.
3. THE Plataforma SHALL permitir modificar un comentario o una nota únicamente a su usuario autor.
4. THE Plataforma SHALL permitir eliminar un comentario o una nota únicamente a su usuario autor.
5. IF un usuario solicita modificar o eliminar un comentario o una nota cuyo autor es otro usuario, THEN THE Plataforma SHALL denegar la operación y registrar en los Logs de Seguridad el identificador del usuario solicitante, el identificador del artefacto y el Identificador de Correlación de la operación.
6. IF un control de autorización aplicable a una operación sobre un artefacto emite una decisión de denegación, THEN THE Plataforma SHALL denegar la operación con independencia de las decisiones afirmativas emitidas por los restantes controles.
7. THE Plataforma SHALL permitir al Administrador de Proyecto propietario de un Proyecto visualizar todos los artefactos de las Review Sessions de ese Proyecto.
8. WHERE un Cliente dispone de una Invitación en estado `Aceptada` sobre un Proyecto que no se encuentra en condición de Proyecto Bloqueado, THE Plataforma SHALL permitir a ese Cliente visualizar todos los artefactos de las Review Sessions de ese Proyecto.

### Requirement 10: Persistencia de artefactos de la Review Session

**User Story:** Como Administrador de Proyecto o Cliente, quiero que toda la información capturada durante una Review Session quede persistida y sea recuperable posteriormente, para poder retomar la revisión sin pérdida de contexto.

#### Acceptance Criteria

1. WHEN una Review Session finaliza la fase de captura, THE Plataforma SHALL persistir de forma duradera todos los artefactos capturados asociados a esa Review Session, incluyendo grabación rrweb, snapshots del DOM, timeline de eventos, metadatos del navegador, escenas Excalidraw, comentarios, notas de texto, notas de voz, transcripciones y artefactos generados por IA.
2. IF la persistencia de los artefactos de una Review Session falla durante la finalización de la captura, THEN THE Plataforma SHALL rechazar la transición de estado de `Recording` a `Completed` de esa Review Session.
3. WHEN un usuario autorizado reabre una Review Session persistida, THE Plataforma SHALL restituir todos los artefactos previamente asociados a esa Review Session.
4. WHILE una Review Session se encuentra en un estado distinto de `Deleted`, THE Plataforma SHALL conservar el historial completo de artefactos asociados a esa Review Session.
5. THE Plataforma SHALL mantener la asociación entre cada artefacto y su Review Session de origen durante toda la existencia del artefacto.
6. WHILE la persistencia de los artefactos de una Review Session permanece en estado fallido, THE Plataforma SHALL mantener disponibles las operaciones sobre las restantes Review Sessions de la Plataforma.
7. WHEN la persistencia de los artefactos de una Review Session se completa satisfactoriamente después de un fallo previo, THE Plataforma SHALL permitir la transición de estado de `Recording` a `Completed` de esa Review Session.
8. IF la persistencia de un artefacto falla, THEN THE Plataforma SHALL descartar ese artefacto sin establecer su asociación con la Review Session.
9. WHILE una Review Session se encuentra en estado `Deleted`, THE Plataforma SHALL denegar toda consulta de los artefactos asociados a esa Review Session.

### Requirement 11: Control de acceso por rol y por Proyecto

**User Story:** Como usuario de la Plataforma, quiero que el acceso al contenido se restrinja según mi rol y mi relación con cada Proyecto, para que ningún usuario acceda a información que no le corresponde.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Administrador de Proyecto acceder únicamente a los Proyectos de los que sea propietario.
2. THE Plataforma SHALL permitir al Cliente acceder únicamente a los Proyectos sobre los que disponga de una Invitación en estado `Aceptada`.
3. THE Plataforma SHALL impedir al Administrador de Plataforma acceder al contenido de Review Sessions.
4. IF una petición autenticada solicita acceso a un recurso del dominio Reviews sin autorización suficiente sobre el Proyecto asociado, THEN THE Plataforma SHALL denegar la petición.
5. WHEN una Invitación de un Cliente sobre un Proyecto transiciona al estado `Revocada`, THE Plataforma SHALL denegar todo acceso posterior de ese Cliente al contenido de ese Proyecto.
6. IF el registro de un intento de acceso no autorizado en los Logs de Seguridad falla, THEN THE Plataforma SHALL mantener la denegación de la petición asociada.

### Requirement 12: Notificaciones de eventos relevantes

**User Story:** Como usuario involucrado en un Proyecto, quiero recibir notificaciones cuando ocurran eventos relevantes en las Review Sessions, para estar al tanto de la actividad sin necesidad de revisar la Plataforma constantemente.

#### Acceptance Criteria

1. WHEN un Cliente agrega un comentario sobre una Review Session, THE Plataforma SHALL generar una notificación dirigida al Administrador de Proyecto propietario del Proyecto asociado.
2. WHEN un Cliente responde un comentario sobre una Review Session, THE Plataforma SHALL generar notificaciones dirigidas al conjunto de destinatarios determinado por las siguientes reglas de consolidación: (a) si el autor del comentario original es distinto del Administrador de Proyecto propietario del Proyecto asociado, THE Plataforma SHALL generar una notificación dirigida al autor del comentario original y una notificación separada dirigida al Administrador de Proyecto propietario; (b) si el autor del comentario original es el Administrador de Proyecto propietario del Proyecto asociado, THE Plataforma SHALL generar una única notificación consolidada dirigida a ese Administrador de Proyecto propietario.
3. WHEN una Review Session transiciona al estado `Completed`, THE Plataforma SHALL generar una notificación dirigida a los Participantes Autorizados del Proyecto asociado.
4. WHEN el procesamiento de IA sobre una Review Session finaliza, THE Plataforma SHALL generar una notificación dirigida al Administrador de Proyecto propietario del Proyecto asociado.
5. THE Plataforma SHALL registrar cada notificación generada en el Historial de Notificaciones del usuario destinatario.
6. IF el registro de una notificación generada en el Historial de Notificaciones falla, THEN THE Plataforma SHALL reintentar ese registro un máximo de 2 veces adicionales al intento original.
7. IF el registro de una notificación en el Historial de Notificaciones falla en el intento original y en los 2 reintentos, THEN THE Plataforma SHALL asignar a esa notificación el Estado de Entrega `Fallida` y exponerla en el Backoffice para diagnóstico.

### Requirement 13: API REST y documentación OpenAPI

**User Story:** Como consumidor técnico (frontend, extensión o integrador), quiero una API REST documentada mediante OpenAPI, para integrarme con la Plataforma de manera predecible y sin ambigüedades.

#### Acceptance Criteria

1. THE Plataforma SHALL exponer sus operaciones de dominio mediante una API REST sobre HTTP.
2. THE Plataforma SHALL publicar una especificación OpenAPI que describa todas las operaciones expuestas por la API REST.
3. WHEN se publica una nueva versión de la API REST, THE Plataforma SHALL actualizar la especificación OpenAPI con las operaciones de esa versión.
4. WHILE una actualización de la especificación OpenAPI se encuentra en curso, THE Plataforma SHALL mantener la API REST disponible para sus consumidores.
5. IF la actualización de la especificación OpenAPI falla tras la publicación de una nueva versión de la API REST, THEN THE Plataforma SHALL mantener la API REST disponible sirviendo la Especificación OpenAPI Vigente.
6. IF se publica una nueva versión de la API REST sin que se realice ningún intento de actualización de la especificación OpenAPI, THEN THE Plataforma SHALL mantener la API REST disponible sirviendo la Especificación OpenAPI Vigente.
7. THE Plataforma SHALL exponer la especificación OpenAPI a través de un endpoint HTTP consumible por herramientas cliente.

### Requirement 14: Internacionalización (i18n)

**User Story:** Como usuario de la Plataforma, quiero que los textos visibles de la interfaz puedan adaptarse a distintos idiomas, para utilizar la Plataforma en el idioma que me resulte natural.

#### Acceptance Criteria

1. THE Plataforma SHALL externalizar todos los textos visibles al usuario en recursos de internacionalización separados del código fuente.
2. THE Plataforma SHALL permitir incorporar nuevos idiomas sin modificar el código fuente de las funcionalidades existentes.
3. WHEN un usuario selecciona un idioma que es un Idioma Completo, THE Plataforma SHALL presentar la totalidad de los textos de la interfaz en ese idioma seleccionado.
4. IF un usuario selecciona un idioma que no es un Idioma Completo, THEN THE Plataforma SHALL presentar la totalidad de los textos de la interfaz en el Idioma por Defecto, sin mezclar claves del idioma seleccionado con claves del Idioma por Defecto.
5. THE Plataforma SHALL presentar los textos de la interfaz en un único idioma en cada presentación de una vista.
6. THE Plataforma SHALL determinar el idioma de presentación de la interfaz únicamente a partir de la disponibilidad de las Traducciones Requeridas del idioma seleccionado, calculando la disponibilidad una sola vez por presentación de vista.

### Requirement 15: Diseño responsive

**User Story:** Como usuario de la Plataforma, quiero que la interfaz sea utilizable tanto en escritorio como en dispositivos móviles, para consultar Review Sessions y comentarios desde cualquier dispositivo con navegador.

#### Acceptance Criteria

1. THE Plataforma SHALL entregar una interfaz web responsive que se adapte a resoluciones de escritorio y de dispositivos móviles.
2. WHEN un usuario accede a la Plataforma desde un dispositivo móvil con navegador web, THE Plataforma SHALL presentar las funcionalidades de consulta y de comentario sobre Review Sessions.

### Requirement 16: Testing incluido para toda funcionalidad (MVP)

**User Story:** Como equipo de desarrollo, quiero que toda funcionalidad de la Plataforma cuente con pruebas, para reducir regresiones y facilitar la evolución del producto.

#### Acceptance Criteria

1. THE Equipo de la Plataforma SHALL entregar pruebas asociadas a cada funcionalidad de la Plataforma dentro del alcance MVP.
2. WHERE no se dispone de un Pipeline de Integración Continua, THE Equipo de la Plataforma SHALL ejecutar de forma manual las pruebas asociadas a una funcionalidad antes de considerar esa funcionalidad terminada.
3. THE Plataforma SHALL organizar su código y su estructura de pruebas de forma que la automatización de la ejecución de pruebas pueda incorporarse sin modificar la estructura de paquetes ni la de los directorios de pruebas.
4. THE Equipo de la Plataforma SHALL registrar, para cada funcionalidad entregada dentro del alcance MVP, la referencia a las pruebas que cubren esa funcionalidad.
5. THE Equipo de la Plataforma SHALL exigir la existencia de pruebas escritas para una funcionalidad como condición previa para considerar esa funcionalidad terminada.

### Requirement 17: Observabilidad desde el inicio

**User Story:** Como responsable operativo de la Plataforma, quiero contar con observabilidad desde el inicio, para diagnosticar incidentes y comprender el comportamiento del sistema en producción.

#### Acceptance Criteria

1. THE Plataforma SHALL emitir logs estructurados de las operaciones ejecutadas por los servicios de dominio.
2. THE Plataforma SHALL exponer métricas de operación consultables por herramientas externas.
3. WHEN se produce un error no controlado durante una operación de dominio, THE Plataforma SHALL registrar ese error con el Identificador de Correlación y el contexto de la operación.
4. IF el mecanismo primario de registro de errores falla y el mecanismo alternativo de registro también falla, THEN THE Plataforma SHALL continuar la ejecución de la operación de dominio en curso sin registrar el error.
5. THE Plataforma SHALL asociar a cada operación un Identificador de Correlación que permita reconstruir su trazabilidad a través de los componentes involucrados.
6. IF el mecanismo primario de registro de errores falla, THEN THE Plataforma SHALL registrar el error mediante el mecanismo alternativo de registro local.
7. IF el mecanismo primario de registro de errores falla, THEN THE Plataforma SHALL continuar la ejecución de la operación de dominio en curso con independencia del resultado del mecanismo alternativo de registro.

### Requirement 18: Seguridad por diseño

**User Story:** Como usuario de la Plataforma, quiero que la seguridad sea una consideración transversal desde el inicio del diseño, para que mis datos y los de mis Clientes queden protegidos.

#### Acceptance Criteria

1. THE Plataforma SHALL requerir autenticación para toda operación que exponga o modifique contenido de Proyectos o de Review Sessions.
2. THE Plataforma SHALL aplicar a toda operación de dominio una autorización basada en el rol del usuario y en la relación de ese usuario con el Proyecto asociado, con independencia del tipo de la operación.
3. WHEN la Plataforma transmite información sensible entre cliente y servidor, THE Plataforma SHALL utilizar canales cifrados.
4. WHEN la Plataforma almacena credenciales de usuario, THE Plataforma SHALL almacenarlas utilizando algoritmos de hashing resistentes a ataques por fuerza bruta.
5. IF una petición no supera las validaciones de autenticación o de autorización, THEN THE Plataforma SHALL denegar la petición y registrar el intento fallido en los Logs de Seguridad.
6. WHEN la autenticación y la autorización de una petición se completan satisfactoriamente, THE Plataforma SHALL registrar el evento en los Logs de Seguridad con fines de auditoría.
7. IF la Plataforma no dispone de una decisión afirmativa de autorización para una petición, THEN THE Plataforma SHALL denegar esa petición.

### Requirement 19: Mantenibilidad, modularidad interna y bajo acoplamiento

**User Story:** Como equipo de desarrollo, quiero que la Plataforma se organice internamente por dominios con responsabilidades cohesivas y bajo acoplamiento, para facilitar la evolución del código sin comprometerme desde el inicio con una separación en módulos de build o servicios independientes.

#### Acceptance Criteria

1. THE Plataforma SHALL organizar su código en paquetes por dominio con responsabilidades cohesivas dentro de un único módulo desplegable.
2. THE Plataforma SHALL mantener un grafo de dependencias entre paquetes de dominio libre de ciclos.
3. THE Equipo de la Plataforma SHALL incorporar una dependencia externa únicamente cuando esa dependencia sea necesaria para satisfacer un requisito funcional o no funcional de este documento.
4. THE Plataforma SHALL restringir el acceso entre paquetes de dominio a las fachadas públicas de cada dominio.
5. THE Plataforma SHALL emitir sus logs y sus métricas a través de interfaces que permitan sustituir el destino de observabilidad sin modificar el código de los servicios de dominio.

### Requirement 20: Decisiones arquitectónicas documentadas mediante ADR

**User Story:** Como miembro actual o futuro del equipo, quiero que toda decisión arquitectónica quede documentada, para comprender el contexto y las consecuencias de las decisiones tomadas.

#### Acceptance Criteria

1. WHEN se adopta una decisión arquitectónica sobre la Plataforma, THE Equipo de la Plataforma SHALL documentar esa decisión mediante un ADR almacenado en el directorio `docs/adr/` del repositorio, con secciones de contexto, alternativas evaluadas, decisión adoptada y consecuencias.
2. THE Equipo de la Plataforma SHALL mantener los ADR versionados junto al código fuente del proyecto.
3. WHEN una decisión arquitectónica previa es sustituida por una nueva decisión, THE Equipo de la Plataforma SHALL registrar el cambio mediante un nuevo ADR que referencie el ADR anterior.

### Requirement 21: Alcance MVP y exclusiones explícitas

**User Story:** Como responsable de producto, quiero que el alcance MVP de la Plataforma quede acotado de forma explícita, para evitar ambigüedades sobre funcionalidades no incluidas en esta etapa.

#### Acceptance Criteria

1. THE Plataforma SHALL excluir del alcance MVP la edición colaborativa en tiempo real sobre Review Sessions.
2. THE Plataforma SHALL excluir del alcance MVP la compartición pública de Review Sessions mediante enlaces.
3. THE Plataforma SHALL excluir del alcance MVP el control de versiones de anotaciones.
4. THE Plataforma SHALL excluir del alcance MVP la provisión de una aplicación móvil nativa.
5. THE Plataforma SHALL excluir del alcance MVP la integración con Jira.
6. THE Plataforma SHALL excluir del alcance MVP la integración con GitHub.
