# Requirements Document

## Introduction

Reviews es una plataforma SaaS orientada a freelancers, agencias y equipos de desarrollo que necesitan revisar aplicaciones web de forma colaborativa. El sistema centraliza el proceso completo de revisión de una aplicación web en una única unidad de trabajo denominada Review Session, eliminando la dispersión de capturas de pantalla, documentos, correos y mensajes.

Durante una Review Session el usuario navega normalmente sobre la aplicación web bajo revisión mientras la Plataforma captura automáticamente la información relevante (grabación rrweb, snapshots del DOM, timeline de eventos, metadatos del navegador, anotaciones Excalidraw, comentarios, notas de texto, notas de voz y artefactos generados por IA). Al finalizar la captura, la sesión queda almacenada de forma persistente y puede reabrirse posteriormente para continuar agregando comentarios y observaciones.

Este documento constituye el spec de **fundación** de la plataforma. Su objetivo es capturar la visión, el modelo de dominio, los actores, las invariantes, los estados del ciclo de vida de una Review Session, y los requisitos no funcionales transversales. Los detalles de cada módulo (modelos de datos concretos, endpoints, pantallas, contratos internos) se desarrollarán en specs posteriores, módulo por módulo, tomando este documento como marco de referencia.

## Glossary

- **Plataforma**: sistema SaaS Reviews en su conjunto (backend, frontend, extensión de navegador autorizada, servicios de soporte).
- **Workspace**: espacio principal donde un Administrador de Proyecto gestiona sus Proyectos y Review Sessions.
- **Proyecto**: agrupación lógica que contiene las Review Sessions asociadas a una aplicación o sitio web bajo revisión.
- **Review Session**: unidad principal de trabajo del dominio; contiene todos los artefactos generados durante una revisión completa.
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
- **Backoffice**: interfaz administrativa de la Plataforma reservada al Administrador de Plataforma.
- **Extensión Autorizada**: extensión de navegador provista por la Plataforma que permite al Cliente iniciar y participar en Review Sessions.
- **Invitación**: mecanismo mediante el cual un Administrador de Proyecto autoriza a un Cliente a acceder a un Proyecto específico.
- **API REST**: interfaz HTTP expuesta por la Plataforma que sigue el estilo arquitectónico REST.
- **OpenAPI**: especificación abierta utilizada para documentar la API REST de la Plataforma.
- **ADR (Architecture Decision Record)**: documento que registra una decisión arquitectónica relevante junto con su contexto, alternativas y consecuencias.
- **Estado de Review Session**: valor del ciclo de vida de una Review Session; toma uno de los valores `Draft`, `Recording`, `Completed`, `Reopened`, `Archived`, `Deleted`.

## Requirements

### Requirement 1: Visión y unidad de trabajo principal

**User Story:** Como Administrador de Proyecto o Cliente, quiero que toda la información de una revisión de aplicación web quede centralizada en una única Review Session, para no depender de capturas de pantalla, correos ni herramientas externas dispersas.

#### Acceptance Criteria

1. THE Plataforma SHALL modelar la Review Session como la unidad principal de trabajo del dominio, agrupando todos los artefactos generados durante una revisión.
2. THE Plataforma SHALL asociar todos los artefactos producidos durante una revisión a la misma Review Session.
3. WHEN un usuario solicita acceder a una Review Session persistida, THE Plataforma SHALL verificar la autorización del usuario sobre la Review Session antes de exponer cualquier artefacto asociado.
4. WHEN la verificación de autorización de un usuario sobre una Review Session persistida resulta satisfactoria, THE Plataforma SHALL exponer únicamente los artefactos efectivamente asociados a esa Review Session.
5. IF una Review Session persistida no dispone aún de artefactos asociados, THEN THE Plataforma SHALL presentar un estado vacío indicando la ausencia de artefactos capturados.
6. THE Plataforma SHALL mantener un único contexto compartido por Review Session para todos los participantes autorizados.

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
4. WHERE un Cliente no dispone de una Invitación aceptada para un Proyecto, THE Plataforma SHALL impedir cualquier acceso de ese Cliente al contenido del Proyecto.
5. WHILE una Invitación de un Cliente sobre un Proyecto permanezca en estado pendiente de aceptación, THE Plataforma SHALL impedir el acceso de ese Cliente al contenido del Proyecto, otorgando acceso únicamente una vez que la Invitación haya sido aceptada.

### Requirement 4: Rol Administrador de Proyecto

**User Story:** Como Administrador de Proyecto, quiero disponer de un conjunto acotado de operaciones sobre mis Proyectos y sus Review Sessions, para gestionar de forma controlada el ciclo de revisión con mis Clientes.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Administrador de Proyecto crear Proyectos sin exigir propiedad previa, asignando automáticamente al usuario creador como propietario del Proyecto recién creado.
2. THE Plataforma SHALL permitir al Administrador de Proyecto editar y eliminar únicamente los Proyectos de los que sea propietario.
3. THE Plataforma SHALL permitir al Administrador de Proyecto invitar Clientes a Proyectos de los que sea propietario.
4. THE Plataforma SHALL permitir al Administrador de Proyecto iniciar Review Sessions sobre Proyectos de los que sea propietario.
5. THE Plataforma SHALL permitir al Administrador de Proyecto cerrar Review Sessions de Proyectos de los que sea propietario, con independencia de que la capacidad de reapertura se encuentre temporalmente disponible o no.
6. WHERE la capacidad de reapertura se encuentre disponible, THE Plataforma SHALL permitir al Administrador de Proyecto reabrir Review Sessions de Proyectos de los que sea propietario.
7. THE Plataforma SHALL permitir al Administrador de Proyecto visualizar todas las Review Sessions de los Proyectos de los que sea propietario.
8. THE Plataforma SHALL permitir al Administrador de Proyecto agregar comentarios y responder comentarios sobre Review Sessions de los Proyectos de los que sea propietario.
9. IF un Administrador de Proyecto intenta acceder a un Proyecto del que no es propietario, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado.

### Requirement 5: Rol Cliente

**User Story:** Como Cliente invitado a un Proyecto, quiero poder participar en las Review Sessions de ese Proyecto tanto desde la aplicación web como desde una Extensión Autorizada, para colaborar sin necesidad de acceso administrativo.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Cliente acceder a la Plataforma mediante la aplicación web y mediante la Extensión Autorizada.
2. WHEN un Cliente dispone de una Invitación aceptada sobre un Proyecto, THE Plataforma SHALL permitir a ese Cliente iniciar Review Sessions sobre ese Proyecto.
3. WHEN un Cliente dispone de una Invitación aceptada sobre un Proyecto, THE Plataforma SHALL permitir a ese Cliente visualizar las Review Sessions de ese Proyecto.
4. WHEN un Cliente dispone de una Invitación aceptada sobre un Proyecto, THE Plataforma SHALL permitir a ese Cliente agregar y responder comentarios sobre las Review Sessions de ese Proyecto.
5. WHEN un Cliente realiza cualquier acceso al contenido de un Proyecto, THE Plataforma SHALL verificar proactivamente que ese Cliente dispone de una Invitación aceptada vigente sobre el Proyecto antes de servir la operación.
6. IF un Cliente intenta acceder a un Proyecto sobre el que no dispone de una Invitación aceptada, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado.
7. IF condiciones internas del sistema requieren la denegación (por ejemplo, indisponibilidad temporal del servicio o estado bloqueado del Proyecto), THEN THE Plataforma SHALL denegar el inicio, la visualización y la incorporación o respuesta de comentarios sobre las Review Sessions del Proyecto por parte del Cliente, aun cuando el Cliente disponga de una Invitación aceptada vigente sobre el Proyecto.

### Requirement 6: Rol Administrador de Plataforma (Backoffice)

**User Story:** Como Administrador de Plataforma, quiero un Backoffice separado de la operación de revisiones, para administrar el SaaS y consultar métricas sin interferir con el contenido de los Proyectos ni de las Review Sessions.

#### Acceptance Criteria

1. THE Plataforma SHALL exponer un Backoffice accesible únicamente al Administrador de Plataforma.
2. THE Plataforma SHALL permitir al Administrador de Plataforma visualizar los Administradores de Proyecto registrados y los Clientes registrados.
3. IF al momento de una consulta del Administrador de Plataforma no existen Administradores de Proyecto registrados o no existen Clientes registrados, THEN THE Plataforma SHALL presentar en el Backoffice listado vacío.
4. THE Plataforma SHALL permitir al Administrador de Plataforma consultar métricas generales de operación de la Plataforma.
5. THE Plataforma SHALL impedir al Administrador de Plataforma participar en Review Sessions.
6. THE Plataforma SHALL impedir al Administrador de Plataforma modificar el contenido de Review Sessions.

### Requirement 7: Invariantes de la Review Session

**User Story:** Como usuario autorizado, quiero reglas de dominio claras sobre la Review Session, para tener certeza de a qué Proyecto pertenece, quién puede accederla y quién puede alterar su ciclo de vida.

#### Acceptance Criteria

1. THE Plataforma SHALL asociar cada Review Session a un único Proyecto durante toda su existencia.
2. THE Plataforma SHALL tratar cada Review Session como un recurso compartido del Proyecto al que pertenece.
3. THE Plataforma SHALL permitir el acceso de un Administrador de Proyecto a las Review Sessions de un Proyecto únicamente cuando dicho usuario disponga tanto del rol de Administrador de Proyecto como de la propiedad verificada del Proyecto asociado, no siendo suficiente ninguna de ambas condiciones por separado y no requiriendo concesiones de acceso adicionales más allá de esas dos.
4. THE Plataforma SHALL permitir que una Review Session sea abierta, cerrada, reabierta y eliminada durante su ciclo de vida.
5. WHEN un usuario solicita cerrar una Review Session, THE Plataforma SHALL verificar el rol y la propiedad del Proyecto antes de otorgar el acceso a la operación, permitiendo el cierre cuando el usuario sea el Administrador de Proyecto propietario verificado del Proyecto al que pertenece, con independencia de otros controles de autorización adicionales.
6. WHEN un usuario solicita eliminar una Review Session, THE Plataforma SHALL verificar el rol y la propiedad del Proyecto antes de otorgar el acceso a la operación, impidiendo incluso el intento de eliminación a cualquier usuario que no sea el Administrador de Proyecto propietario del Proyecto al que pertenece.
7. IF un usuario que no es el Administrador de Proyecto propietario intenta cerrar o eliminar una Review Session, THEN THE Plataforma SHALL denegar la operación y registrar el intento de acceso no autorizado.

### Requirement 8: Ciclo de vida y estados de la Review Session

**User Story:** Como Administrador de Proyecto o Cliente autorizado, quiero que la Review Session tenga un ciclo de vida explícito con estados bien definidos, para saber en todo momento qué operaciones están disponibles sobre ella.

#### Acceptance Criteria

1. THE Plataforma SHALL asignar a cada Review Session exactamente uno de los siguientes estados en cualquier instante: `Draft`, `Recording`, `Completed`, `Reopened`, `Archived`, `Deleted`.
2. WHEN una Review Session es creada, THE Plataforma SHALL inicializar su estado en `Draft`.
3. WHEN la captura de una Review Session inicia y su estado actual es `Draft`, THE Plataforma SHALL transicionar su estado de `Draft` a `Recording`; en cualquier otro estado la Plataforma SHALL rechazar el inicio de captura.
4. WHEN la captura de una Review Session finaliza y su estado actual es `Recording`, THE Plataforma SHALL transicionar su estado de `Recording` a `Completed`; en cualquier otro estado la Plataforma SHALL rechazar la finalización de captura.
5. WHEN el Administrador de Proyecto propietario solicita reabrir una Review Session, THE Plataforma SHALL validar tanto la acción de reapertura como el estado actual, y SHALL transicionar el estado a `Reopened` únicamente si la Review Session se encuentra en estado `Completed`.
6. WHEN una Review Session es archivada, THE Plataforma SHALL transicionar su estado a `Archived`.
7. WHEN una Review Session es eliminada por el Administrador de Proyecto propietario, THE Plataforma SHALL transicionar su estado a `Deleted`.
8. WHILE una Review Session se encuentra en estado `Recording`, THE Plataforma SHALL permitir la captura de artefactos asociados a esa Review Session.
9. WHILE una Review Session se encuentra en estado `Completed` o `Reopened`, THE Plataforma SHALL permitir agregar comentarios, notas y anotaciones sin capturar nuevos eventos de la aplicación web bajo revisión.
10. WHILE una Review Session se encuentra en estado `Draft`, THE Plataforma SHALL exponerla en los listados activos del Proyecto y permitir sobre ella el conjunto completo de operaciones aplicables antes de la captura.
11. WHEN el estado de una Review Session transiciona a `Deleted`, THE Plataforma SHALL aplicar de forma inmediata las restricciones de modificación y de aparición en los listados de Review Sessions activas del Proyecto, aun cuando el proceso de eliminación todavía no haya finalizado por completo.

### Requirement 9: Autoría y edición de artefactos

**User Story:** Como usuario que produce artefactos dentro de una Review Session, quiero que mi autoría quede registrada de forma permanente y que solo yo pueda modificar o eliminar mis propios comentarios y notas, para preservar la trazabilidad y la integridad de la revisión.

#### Acceptance Criteria

1. WHEN un artefacto es creado dentro de una Review Session, THE Plataforma SHALL registrar de forma permanente al usuario autor del artefacto.
2. THE Plataforma SHALL conservar el registro de autoría de cada artefacto durante toda la vida de la Review Session a la que pertenece.
3. THE Plataforma SHALL permitir modificar un comentario o una nota únicamente a su usuario autor.
4. THE Plataforma SHALL permitir eliminar un comentario o una nota únicamente a su usuario autor.
5. IF un usuario intenta modificar o eliminar un comentario o una nota cuyo autor no es él, THEN THE Plataforma SHALL denegar la operación y SHALL registrar el intento únicamente cuando el mismo constituya una amenaza real de seguridad, evitando registrar denegaciones triviales cubiertas por controles rutinarios.
6. IF cualquier control del sistema deniega una operación de modificación o eliminación sobre un artefacto, THEN THE Plataforma SHALL bloquear la operación con precedencia sobre cualquier señal de autorización concurrente, garantizando que la denegación siempre prevalece sobre la autorización.
7. THE Plataforma SHALL permitir al Administrador de Proyecto propietario del Proyecto visualizar todos los artefactos de las Review Sessions de ese Proyecto.
8. THE Plataforma SHALL permitir a Cliente del Proyecto visualizar todos los artefactos de las Review Sessions de ese Proyecto.

### Requirement 10: Persistencia de artefactos de la Review Session

**User Story:** Como Administrador de Proyecto o Cliente, quiero que toda la información capturada durante una Review Session quede persistida y sea recuperable posteriormente, para poder retomar la revisión sin pérdida de contexto.

#### Acceptance Criteria

1. WHEN una Review Session finaliza la fase de captura, THE Plataforma SHALL persistir de forma duradera todos los artefactos capturados asociados a esa Review Session, incluyendo grabación rrweb, snapshots del DOM, timeline de eventos, metadatos del navegador, escenas Excalidraw, comentarios, notas de texto, notas de voz, transcripciones y artefactos generados por IA.
2. IF la persistencia de artefactos falla por problemas de almacenamiento o de red durante la finalización de captura, THEN THE Plataforma SHALL bloquear la transición de estado y las operaciones subsiguientes exclusivamente sobre la Review Session cuya persistencia falló, sin afectar a otras Review Sessions sobre las que el mismo usuario u otros usuarios puedan operar, hasta que la persistencia se complete satisfactoriamente.
3. WHEN un usuario autorizado reabre una Review Session persistida, THE Plataforma SHALL restituir todos los artefactos previamente asociados a esa Review Session.
4. WHILE una Review Session no se encuentra en estado `Deleted`, THE Plataforma SHALL conservar el historial completo de artefactos asociados a esa Review Session.
5. THE Plataforma SHALL mantener la asociación entre cada artefacto y su Review Session de origen durante toda la existencia del artefacto, e IF la persistencia del artefacto falla, THEN THE Plataforma SHALL impedir el establecimiento de la asociación.

### Requirement 11: Control de acceso por rol y por Proyecto

**User Story:** Como usuario de la Plataforma, quiero que el acceso al contenido se restrinja según mi rol y mi relación con cada Proyecto, para que ningún usuario acceda a información que no le corresponde.

#### Acceptance Criteria

1. THE Plataforma SHALL permitir al Administrador de Proyecto acceder únicamente a los Proyectos de los que sea propietario.
2. THE Plataforma SHALL permitir al Cliente acceder únicamente a los Proyectos para los cuales disponga de una Invitación aceptada.
3. THE Plataforma SHALL impedir al Administrador de Plataforma acceder al contenido de Review Sessions.
4. IF una petición autenticada intenta acceder a un recurso del dominio Reviews sin autorización suficiente sobre el Proyecto asociado, THEN THE Plataforma SHALL denegar la petición aun si el registro del intento de acceso no autorizado falla por razones técnicas.
5. WHEN una Invitación de un Cliente sobre un Proyecto es revocada, THE Plataforma SHALL impedir todo acceso subsiguiente de ese Cliente al contenido de ese Proyecto.

### Requirement 12: Notificaciones de eventos relevantes

**User Story:** Como usuario involucrado en un Proyecto, quiero recibir notificaciones cuando ocurran eventos relevantes en las Review Sessions, para estar al tanto de la actividad sin necesidad de revisar la Plataforma constantemente.

#### Acceptance Criteria

1. WHEN un Cliente agrega un comentario sobre una Review Session, THE Plataforma SHALL generar una notificación dirigida al Administrador de Proyecto propietario del Proyecto asociado.
2. WHEN un Cliente responde un comentario sobre una Review Session, THE Plataforma SHALL generar una notificación dirigida al autor del comentario original y al Administrador de Proyecto propietario del Proyecto asociado; IF el autor del comentario original coincide con el Administrador de Proyecto propietario, THEN THE Plataforma SHALL generar una única notificación consolidada.
3. WHEN una Review Session finaliza y transiciona al estado `Completed`, THE Plataforma SHALL generar una notificación dirigida a los participantes autorizados del Proyecto asociado.
4. WHEN el procesamiento de IA sobre una Review Session finaliza, THE Plataforma SHALL generar una notificación dirigida al Administrador de Proyecto propietario del Proyecto asociado.
5. THE Plataforma SHALL registrar cada notificación generada de forma consultable por el usuario destinatario dentro de la Plataforma.
6. IF la generación de una notificación se completa satisfactoriamente pero el registro en el historial consultable por el destinatario falla, THEN THE Plataforma SHALL reintentar dicho registro hasta 2 intentos.

### Requirement 13: API REST y documentación OpenAPI

**User Story:** Como consumidor técnico (frontend, extensión o integrador), quiero una API REST documentada mediante OpenAPI, para integrarme con la Plataforma de manera predecible y sin ambigüedades.

#### Acceptance Criteria

1. THE Plataforma SHALL exponer sus operaciones de dominio mediante una API REST sobre HTTP.
2. THE Plataforma SHALL publicar una especificación OpenAPI que describa todas las operaciones expuestas por la API REST.
3. WHEN se publica una nueva versión de la API REST, THE Plataforma SHALL iniciar la actualización de la especificación OpenAPI para reflejar los cambios.
4. WHILE se aplica una actualización de la especificación OpenAPI, THE Plataforma SHALL mantener la API REST operativa para sus consumidores, tanto si la actualización culmina de manera satisfactoria como si falla.
5. IF la actualización de la especificación OpenAPI falla tras la publicación de una nueva versión de la API REST, THEN THE Plataforma SHALL mantener la API operativa con la especificación desactualizada hasta que la actualización pueda completarse satisfactoriamente.
6. WHEN se publica una nueva versión de la API REST sin que se realice ningún intento de actualización de la especificación OpenAPI, THE Plataforma SHALL mantener la API operativa con la especificación anterior.
7. THE Plataforma SHALL exponer la especificación OpenAPI a través de un endpoint HTTP consumible por herramientas cliente.

### Requirement 14: Internacionalización (i18n)

**User Story:** Como usuario de la Plataforma, quiero que los textos visibles de la interfaz puedan adaptarse a distintos idiomas, para utilizar la Plataforma en el idioma que me resulte natural.

#### Acceptance Criteria

1. THE Plataforma SHALL externalizar todos los textos visibles al usuario en recursos de internacionalización.
2. THE Plataforma SHALL permitir incorporar nuevos idiomas sin modificar el código fuente de las funcionalidades existentes.
3. WHEN un usuario selecciona un idioma cuyas traducciones se encuentran completas, THE Plataforma SHALL presentar la totalidad de los textos de la interfaz en el idioma seleccionado, incluso si dicho idioma no forma parte del conjunto de idiomas oficialmente soportados; IF alguna traducción requerida para el idioma seleccionado no está disponible (traducciones incompletas), THEN THE Plataforma SHALL presentar la totalidad de la interfaz en el idioma por defecto en lugar de combinar idiomas.

### Requirement 15: Diseño responsive

**User Story:** Como usuario de la Plataforma, quiero que la interfaz sea utilizable tanto en escritorio como en dispositivos móviles, para consultar Review Sessions y comentarios desde cualquier dispositivo con navegador.

#### Acceptance Criteria

1. THE Plataforma SHALL entregar una interfaz web responsive que se adapte a resoluciones de escritorio y móviles.
2. WHEN un usuario accede a la Plataforma desde un dispositivo móvil con navegador web, THE Plataforma SHALL presentar las funcionalidades de consulta y comentario sobre Review Sessions.

### Requirement 16: Testing incluido para toda funcionalidad (MVP)

**User Story:** Como equipo de desarrollo, quiero que toda funcionalidad de la Plataforma cuente con pruebas, para reducir regresiones y facilitar la evolución del producto.

#### Acceptance Criteria

1. THE Plataforma SHALL incluir pruebas asociadas a cada funcionalidad entregada.
2. WHERE en la fase MVP no se dispone de pipeline de integración continua, THE Plataforma SHALL ejecutar las pruebas de forma manual antes de considerar una funcionalidad como terminada, requiriendo que existan pruebas escritas para dicha funcionalidad como condición previa a su ejecución.
3. THE Plataforma SHALL organizar su código y su estructura de pruebas de forma que permita incorporar automatización de ejecución de pruebas en fases posteriores sin refactorización estructural.
4. WHERE una funcionalidad se desarrolla en el marco de una fase de prototipado rápido o de prueba de concepto explícitamente identificada como tal, THE Plataforma SHALL permitir marcar dicha funcionalidad como completada sin pruebas asociadas, sin que esta excepción se extienda a las funcionalidades entregadas dentro del alcance MVP.

### Requirement 17: Observabilidad desde el inicio

**User Story:** Como responsable operativo de la Plataforma, quiero contar con observabilidad desde el inicio, para diagnosticar incidentes y comprender el comportamiento del sistema en producción.

#### Acceptance Criteria

1. THE Plataforma SHALL emitir logs estructurados de las operaciones relevantes ejecutadas por los servicios de dominio.
2. THE Plataforma SHALL exponer métricas de operación consultables por herramientas externas.
3. WHEN se produce un error no controlado durante una operación de dominio, THE Plataforma SHALL registrar el error con la información de contexto necesaria para su diagnóstico; IF el mecanismo primario de registro de errores falla, THEN THE Plataforma SHALL emplear un mecanismo alternativo (por ejemplo, registro en archivo local) en paralelo para preservar el registro del error, mientras continúa procesando la operación original sin interrumpirla.
4. IF tras un fallo del mecanismo primario de registro de errores el mecanismo alternativo también falla, THEN THE Plataforma SHALL continuar la operación original sin registrar el error asociado y sin bloquear su ejecución.
5. THE Plataforma SHALL asociar a cada operación un identificador de correlación que permita reconstruir su trazabilidad a través de los componentes involucrados.

### Requirement 18: Seguridad por diseño

**User Story:** Como usuario de la Plataforma, quiero que la seguridad sea una consideración transversal desde el inicio del diseño, para que mis datos y los de mis Clientes queden protegidos.

#### Acceptance Criteria

1. THE Plataforma SHALL requerir autenticación para toda operación que exponga o modifique contenido de Proyectos o de Review Sessions.
2. THE Plataforma SHALL aplicar autorización basada en rol y en la relación del usuario con el Proyecto asociado a toda operación de dominio, con independencia de su tipo (lectura, escritura, consulta o cualquier otro), sin distinguir entre operaciones que exponen contenido y operaciones que lo modifican.
3. WHEN la Plataforma transmite información sensible entre cliente y servidor, THE Plataforma SHALL utilizar canales cifrados.
4. WHEN la Plataforma almacena credenciales de usuario, THE Plataforma SHALL almacenarlas utilizando algoritmos de hashing resistentes a ataques por fuerza bruta.
5. IF una petición no supera las validaciones de autenticación o autorización, THEN THE Plataforma SHALL denegar la petición y registrar el intento fallido en los logs de seguridad.
6. WHEN una autenticación o una autorización se completa satisfactoriamente sobre una petición, THE Plataforma SHALL registrar el evento exitoso en los logs de seguridad con fines de auditoría.
7. IF por errores de implementación o de configuración la Plataforma no ha aplicado la autorización correspondiente sobre una petición, THEN THE Plataforma SHALL denegar la petición aun cuando la misma hubiera resultado autorizada de haberse aplicado la autorización correctamente.

### Requirement 19: Mantenibilidad, modularidad interna y bajo acoplamiento

**User Story:** Como equipo de desarrollo, quiero que la Plataforma se organice internamente por dominios con responsabilidades cohesivas y bajo acoplamiento, para facilitar la evolución del código sin comprometerme desde el inicio con una separación en módulos de build o servicios independientes.

#### Acceptance Criteria

1. THE Plataforma SHALL organizar su código en paquetes por dominio con responsabilidades cohesivas dentro de un único módulo desplegable.
2. THE Plataforma SHALL mantener el acoplamiento entre paquetes de dominio acotado, evitando dependencias circulares y minimizando el uso de tipos internos de un dominio desde otro.
3. THE Plataforma SHALL evitar incorporar dependencias externas que no sean necesarias para satisfacer un requisito funcional o no funcional.

### Requirement 20: Decisiones arquitectónicas documentadas mediante ADR

**User Story:** Como miembro actual o futuro del equipo, quiero que toda decisión arquitectónica quede documentada, para comprender el contexto y las consecuencias de las decisiones tomadas.

#### Acceptance Criteria

1. WHEN se adopta cualquier decisión arquitectónica sobre la Plataforma, sin importar su magnitud, THE equipo de la Plataforma SHALL documentar la decisión mediante un Architecture Decision Record.
2. THE Plataforma SHALL mantener los Architecture Decision Records versionados junto al código fuente del proyecto.
3. WHEN una decisión arquitectónica previa es sustituida por una nueva, THE equipo de la Plataforma SHALL registrar el cambio mediante un nuevo Architecture Decision Record que referencie el anterior.

### Requirement 21: Alcance MVP y exclusiones explícitas

**User Story:** Como responsable de producto, quiero que el alcance MVP de la Plataforma quede acotado de forma explícita, para evitar ambigüedades sobre funcionalidades no incluidas en esta etapa.

#### Acceptance Criteria

1. THE Plataforma SHALL excluir del alcance MVP la edición colaborativa en tiempo real sobre Review Sessions.
2. THE Plataforma SHALL excluir del alcance MVP la compartición pública de Review Sessions mediante enlaces.
3. THE Plataforma SHALL excluir del alcance MVP el control de versiones de anotaciones.
4. THE Plataforma SHALL excluir del alcance MVP la provisión de una aplicación móvil nativa.
5. THE Plataforma SHALL excluir del alcance MVP la integración con Jira.
6. THE Plataforma SHALL excluir del alcance MVP la integración con GitHub.
7. THE Plataforma SHALL excluir del alcance MVP la funcionalidad de OCR sobre artefactos.
8. THE Plataforma SHALL excluir del alcance MVP la detección automática de bugs mediante IA.
