# Requirements Document

## Introduction

El **Entorno de Desarrollo Local** provee a los desarrolladores de la plataforma Reviews un entorno reproducible, portátil y autocontenido para levantar en su máquina personal los servicios de backend y base de datos necesarios para trabajar sobre el producto. El objetivo es reducir la fricción de onboarding, garantizar paridad entre máquinas y sistemas operativos (Windows y Unix), habilitar ciclos de desarrollo rápidos mediante recarga automática de cambios y depuración remota del backend, y permitir la ejecución de pruebas manuales de la API HTTP desde herramientas locales del Desarrollador (Postman, `curl`, HTTPie, IntelliJ HTTP Client) sin necesidad de un frontend.

Este feature entrega la orquestación (Docker Compose) de dos Servicios coordinados en una red interna de Docker: la base de datos PostgreSQL y la aplicación Spring Boot en modo desarrollo. Incluye además las imágenes de desarrollo de cada Servicio, la configuración parametrizada por variables de entorno, los scripts operativos multiplataforma para las acciones frecuentes, y la documentación de onboarding orientada a un flujo de trabajo backend-first (API + BD + Postman). Queda fuera del alcance del feature: el Servicio de frontend y su tooling asociado (dev server, HMR, bundler); las imágenes de producción; los pipelines de integración continua; los despliegues a entornos remotos; y el contenido funcional del backend (que se define en specs propios). Los prerequisitos del host (Docker Engine, Docker Compose V2) se documentan como parte del feature pero no se instalan automáticamente.

El Servicio de frontend queda explícitamente diferido a un spec futuro. Cuando se incorpore, deberá extender la red interna, el Archivo_Env y los Comandos_Operativos existentes sin invalidar las decisiones tomadas aquí.

Este documento se alinea con el spec de fundación `reviews-platform-foundation` en cuanto a seguridad por diseño (Requirement 18 de la fundación), observabilidad transversal (Requirement 17 de la fundación) y modularidad y bajo acoplamiento (Requirement 19 de la fundación).

## Glossary

- **Entorno_Local**: sistema completo de desarrollo local definido por este spec, orquestado por Docker Compose.
- **Docker_Compose**: herramienta de orquestación de contenedores que consume la definición declarativa del Entorno_Local y coordina el ciclo de vida de sus servicios.
- **Desarrollador**: usuario humano que instala, arranca, usa o mantiene el Entorno_Local en su máquina personal.
- **Servicio**: unidad de ejecución del Entorno_Local encapsulada en un contenedor Docker independiente; el Entorno_Local incluye exactamente dos servicios: Servicio_Backend y Servicio_Postgres.
- **Servicio_Backend**: contenedor que ejecuta la aplicación Spring Boot en modo de desarrollo, con recarga automática de cambios y con canal de depuración remota expuesto.
- **Servicio_Postgres**: contenedor que ejecuta PostgreSQL 16 como base de datos del entorno.
- **Red_Interna**: red bridge de Docker (`reviews-net`) que conecta los dos servicios entre sí por nombre DNS.
- **Volumen_Persistente**: volumen nombrado gestionado por Docker, usado para almacenar datos que deben sobrevivir a los ciclos de detención y arranque del Entorno_Local.
- **Bind_Mount**: mapeo del filesystem del host al contenedor, usado para exponer el código fuente del repositorio dentro del Servicio_Backend.
- **Archivo_Env**: archivo `.env` (no versionado) que contiene los valores concretos de las variables de configuración del Entorno_Local en la máquina del Desarrollador.
- **Plantilla_Env**: archivo `.env.example` (versionado) que documenta las variables de configuración soportadas y sus valores por defecto no sensibles.
- **Comando_Operativo**: script cross-platform provisto por el Entorno_Local para una operación frecuente sobre el ciclo de vida (arrancar, detener, reiniciar borrando datos, ver logs, abrir shell).
- **Healthcheck**: sonda periódica declarada por un Servicio para reportar a Docker si está listo para recibir tráfico.
- **Hot_Reload_Backend**: recarga automática del contexto del Servicio_Backend al detectar cambios en las clases compiladas del código fuente montado.
- **JDWP**: Java Debug Wire Protocol; canal TCP publicado por el Servicio_Backend para adjuntar un debugger remoto desde el IDE del Desarrollador.
- **Puerto_Publicado**: puerto TCP del host al que Docker mapea un puerto interno de un Servicio, para permitir el acceso desde herramientas locales del Desarrollador.
- **Reset**: operación explícita que detiene el Entorno_Local y elimina los Volúmenes_Persistentes, dejando el estado en las condiciones iniciales de una primera ejecución.
- **Migración_Flyway**: script SQL versionado del backend que Flyway aplica al arrancar el Servicio_Backend para llevar la base de datos al esquema esperado.
- **Cliente_HTTP_Local**: herramienta ejecutándose en el host del Desarrollador que consume la API del Servicio_Backend a través del Puerto_Publicado documentado (ejemplos: Postman, `curl`, HTTPie, IntelliJ HTTP Client, Bruno, Insomnia).
- **Coleccion_Postman**: colección versionada de peticiones HTTP contra la API del Servicio_Backend, importable en Postman u otras herramientas compatibles, provista por el Entorno_Local como material de onboarding.

## Requirements

### Requirement 1: Arranque completo con un único Comando_Operativo

**User Story:** Como Desarrollador, quiero levantar todos los servicios del Entorno_Local con un único Comando_Operativo, para completar el arranque sin ejecutar pasos manuales adicionales por cada servicio.

#### Acceptance Criteria

1. THE Entorno_Local SHALL exponer un Comando_Operativo documentado que arranca los dos Servicios (Servicio_Postgres y Servicio_Backend) en una única invocación.
2. WHEN el Desarrollador ejecuta el Comando_Operativo de arranque documentado desde la raíz del repositorio, THE Entorno_Local SHALL orquestar la creación de los dos Servicios sin exigir al Desarrollador ejecutar ningún otro comando durante el arranque.
3. WHEN todos los Servicios reportan estado saludable tras el arranque, THE Entorno_Local SHALL exponer el Servicio_Backend y el Servicio_Postgres en Puertos_Publicados documentados en la Plantilla_Env.
4. IF el Docker Engine no está en ejecución en la máquina del Desarrollador en el momento de invocar el Comando_Operativo de arranque, THEN THE Entorno_Local SHALL abortar el arranque con un mensaje de error que identifica al Docker Engine como prerequisito faltante.
5. WHEN el Desarrollador re-invoca el Comando_Operativo de arranque mientras los Servicios ya se encuentran en ejecución, THE Entorno_Local SHALL reconciliar el estado sin duplicar contenedores, volúmenes ni redes.

### Requirement 2: Aislamiento por contenedor para cada Servicio

**User Story:** Como Desarrollador, quiero que cada Servicio del Entorno_Local se ejecute en un contenedor independiente, para que un fallo o reinicio de un Servicio no derribe el otro.

#### Acceptance Criteria

1. THE Entorno_Local SHALL ejecutar el Servicio_Postgres y el Servicio_Backend en dos contenedores Docker independientes, cada uno con su propio ciclo de vida.
2. WHEN el Servicio_Backend termina de forma inesperada, THE Entorno_Local SHALL mantener al Servicio_Postgres en ejecución sin interrumpir su estado.
3. WHEN el Servicio_Postgres termina de forma inesperada, THE Entorno_Local SHALL mantener al Servicio_Backend en ejecución sin interrumpir su estado, aun cuando el Servicio_Backend no pueda atender peticiones que dependan de la base de datos.
4. WHEN un Servicio se reinicia individualmente por instrucción del Desarrollador, THE Entorno_Local SHALL reiniciar únicamente ese Servicio sin reiniciar el otro.

### Requirement 3: Persistencia de datos de base de datos entre reinicios

**User Story:** Como Desarrollador, quiero que los datos escritos en la base de datos sobrevivan a los ciclos de detención y arranque del Entorno_Local, para no perder mi trabajo local al reiniciar la máquina o los contenedores.

#### Acceptance Criteria

1. THE Entorno_Local SHALL almacenar el directorio de datos del Servicio_Postgres en un Volumen_Persistente nombrado por Docker.
2. WHEN el Desarrollador ejecuta el Comando_Operativo de detención sin la opción de borrado de volúmenes, THE Entorno_Local SHALL preservar íntegramente el Volumen_Persistente de datos del Servicio_Postgres.
3. WHEN el Desarrollador re-arranca el Entorno_Local tras una detención sin borrado de volúmenes, THE Entorno_Local SHALL exponer al Servicio_Postgres con los datos previamente persistidos.
4. WHEN el Desarrollador ejecuta el Comando_Operativo de Reset, THE Entorno_Local SHALL eliminar el Volumen_Persistente de datos del Servicio_Postgres junto con los demás Volúmenes_Persistentes del proyecto.
5. WHEN el Desarrollador arranca el Entorno_Local tras un Reset, THE Entorno_Local SHALL inicializar el Servicio_Postgres desde cero, ejecutando los scripts de inicialización presentes en el directorio de inicialización del Servicio_Postgres.

### Requirement 4: Recarga automática del Servicio_Backend ante cambios de código

**User Story:** Como Desarrollador, quiero que el Servicio_Backend refleje mis cambios de código sin reconstruir la imagen ni reiniciar manualmente el contenedor, para acortar el ciclo de edición y prueba del backend.

#### Acceptance Criteria

1. THE Entorno_Local SHALL montar el directorio `backend/` del host como Bind_Mount dentro del Servicio_Backend.
2. THE Entorno_Local SHALL activar el mecanismo de Hot_Reload_Backend basado en Spring DevTools durante la ejecución del Servicio_Backend.
3. WHEN el Desarrollador guarda un cambio en un archivo fuente Java del directorio `backend/src` del host, THE Entorno_Local SHALL propagar el cambio al Servicio_Backend a través del Bind_Mount sin requerir un rebuild de la imagen.
4. WHEN el Servicio_Backend detecta clases modificadas en su classpath, THE Entorno_Local SHALL reiniciar el contexto del backend automáticamente mediante Spring DevTools.
5. WHEN los cambios se limitan a archivos `pom.xml` u otros descriptores de build de Maven, THE Entorno_Local SHALL documentar en la guía de onboarding que el Desarrollador debe reiniciar el Servicio_Backend explícitamente para que las nuevas dependencias entren en el classpath.

### Requirement 5: Depuración remota del Servicio_Backend

**User Story:** Como Desarrollador, quiero adjuntar el debugger de mi IDE al Servicio_Backend, para inspeccionar el estado del backend en ejecución dentro del contenedor.

#### Acceptance Criteria

1. THE Entorno_Local SHALL habilitar el agente JDWP en el Servicio_Backend durante toda su ejecución en modo desarrollo.
2. THE Entorno_Local SHALL publicar el puerto JDWP del Servicio_Backend en un Puerto_Publicado configurable a través de la Plantilla_Env, con valor por defecto 5005.
3. THE Entorno_Local SHALL configurar el agente JDWP en modo no-suspendido para no bloquear el arranque del Servicio_Backend cuando no haya un debugger adjunto.
4. WHEN el Desarrollador conecta un debugger al Puerto_Publicado del JDWP, THE Entorno_Local SHALL permitir la sesión de depuración remota sin reiniciar el Servicio_Backend.

### Requirement 6: Comunicación entre Servicios por Red_Interna

**User Story:** Como Desarrollador, quiero que los Servicios se descubran y comuniquen entre sí sin depender de los Puertos_Publicados en el host, para que el Entorno_Local funcione con independencia de qué puertos estén ocupados en la máquina.

#### Acceptance Criteria

1. THE Entorno_Local SHALL crear una Red_Interna bridge denominada `reviews-net` a la cual adjunta a los dos Servicios.
2. WHEN el Servicio_Backend se comunica con el Servicio_Postgres, THE Entorno_Local SHALL permitir la resolución del Servicio_Postgres por el nombre DNS `postgres` en la Red_Interna sobre el puerto 5432.
3. THE Entorno_Local SHALL exponer al host los Servicios exclusivamente a través de los Puertos_Publicados declarados en la definición del compose, sin exponer puertos adicionales por servicio.
4. THE Entorno_Local SHALL reservar la extensibilidad de la Red_Interna para permitir la incorporación futura de otros Servicios (por ejemplo un Servicio de frontend) sin renombrar la red ni cambiar el nombre DNS del Servicio_Backend ni del Servicio_Postgres.

### Requirement 7: Arranque ordenado en función de Healthchecks

**User Story:** Como Desarrollador, quiero que el Servicio_Backend arranque únicamente después de que el Servicio_Postgres esté listo para aceptar conexiones, para evitar fallos transitorios de conexión al iniciar el entorno.

#### Acceptance Criteria

1. THE Entorno_Local SHALL declarar un Healthcheck para el Servicio_Postgres basado en la comprobación `pg_isready` contra la base de datos configurada.
2. THE Entorno_Local SHALL declarar un Healthcheck para el Servicio_Backend basado en la respuesta del endpoint HTTP `/actuator/health`.
3. THE Entorno_Local SHALL configurar el arranque del Servicio_Backend con una dependencia explícita al estado saludable del Servicio_Postgres, mediante `depends_on` con `condition: service_healthy`.
4. WHILE el Servicio_Postgres no reporta estado saludable, THE Entorno_Local SHALL mantener al Servicio_Backend en estado creado sin iniciar el proceso Spring Boot.
5. IF el Servicio_Postgres no alcanza el estado saludable dentro del `start_period` configurado en su Healthcheck, THEN THE Entorno_Local SHALL reportar al Servicio_Postgres como `unhealthy` y no iniciar el Servicio_Backend.

### Requirement 8: Configuración parametrizada por variables de entorno

**User Story:** Como Desarrollador, quiero configurar credenciales, puertos y otros parámetros del Entorno_Local desde un único Archivo_Env, para adaptar el entorno a mi máquina sin editar la definición del compose.

#### Acceptance Criteria

1. THE Entorno_Local SHALL versionar en el repositorio una Plantilla_Env que documenta todas las variables de configuración soportadas y valores por defecto no sensibles.
2. THE Entorno_Local SHALL leer los valores efectivos de configuración desde un Archivo_Env instanciado por el Desarrollador a partir de la Plantilla_Env.
3. THE Entorno_Local SHALL soportar las siguientes variables de configuración a través de la Plantilla_Env: `COMPOSE_PROJECT_NAME`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST_PORT`, `BACKEND_HOST_PORT`, `BACKEND_DEBUG_PORT`, `SPRING_PROFILES_ACTIVE` y `BACKEND_JAVA_OPTS`.
4. IF la variable `POSTGRES_PASSWORD` no está definida o está vacía en el Archivo_Env en el momento del arranque, THEN THE Entorno_Local SHALL abortar el arranque con un mensaje de error que identifica a `POSTGRES_PASSWORD` como variable obligatoria.
5. WHEN un valor de Puerto_Publicado configurado en el Archivo_Env está dentro del rango de puertos válido no privilegiado, THE Entorno_Local SHALL usar ese valor como Puerto_Publicado del Servicio correspondiente.
6. WHEN una variable soportada por la Plantilla_Env no está presente en el Archivo_Env, THE Entorno_Local SHALL usar el valor por defecto documentado en la Plantilla_Env para esa variable.

### Requirement 9: Higiene de secretos y credenciales

**User Story:** Como Desarrollador, quiero que ninguna credencial real ni valor sensible quede versionado en el repositorio, para evitar filtraciones accidentales al empujar cambios al remoto.

#### Acceptance Criteria

1. THE Entorno_Local SHALL declarar el Archivo_Env como archivo ignorado por Git en el `.gitignore` del repositorio.
2. THE Entorno_Local SHALL asegurar que la Plantilla_Env no contiene credenciales reales, usando marcadores explícitos no explotables (por ejemplo `CHANGE_ME`) para toda variable sensible.
3. THE Entorno_Local SHALL asegurar que la definición del compose no contiene valores literales de `POSTGRES_PASSWORD` u otras credenciales, referenciando esos valores exclusivamente a través de variables de entorno.
4. IF un archivo versionado incluye un valor literal que coincide con el patrón de una credencial declarada como sensible en la Plantilla_Env, THEN THE Entorno_Local SHALL fallar la verificación de higiene de secretos declarada en la guía de contribución del feature.

### Requirement 10: Comandos_Operativos cross-platform

**User Story:** Como Desarrollador que trabaja en Windows o en un sistema Unix, quiero disponer de los mismos Comandos_Operativos en ambos sistemas, para no aprender flags distintos según mi máquina.

#### Acceptance Criteria

1. THE Entorno_Local SHALL proveer un script `.sh` ejecutable en shells POSIX y un script `.cmd` ejecutable en `cmd.exe` para cada uno de los siguientes Comandos_Operativos: arrancar, detener, reset y ver logs.
2. THE Entorno_Local SHALL asegurar que los scripts `.sh` y los scripts `.cmd` correspondientes al mismo Comando_Operativo producen el mismo efecto observable sobre el estado de los Servicios y los Volúmenes_Persistentes.
3. WHEN el Desarrollador invoca el Comando_Operativo de arranque y no existe el Archivo_Env, THE Entorno_Local SHALL crear el Archivo_Env copiando la Plantilla_Env y SHALL detener la ejecución solicitando al Desarrollador editar el Archivo_Env antes de reintentar.
4. WHEN el Desarrollador invoca el Comando_Operativo de Reset, THE Entorno_Local SHALL solicitar confirmación explícita antes de eliminar los Volúmenes_Persistentes.
5. WHEN el Desarrollador invoca el Comando_Operativo de logs con el nombre de un Servicio, THE Entorno_Local SHALL emitir en modo seguimiento continuo los logs del Servicio indicado; y WHEN el Desarrollador invoca el Comando_Operativo de logs sin nombre de Servicio, THE Entorno_Local SHALL emitir en modo seguimiento continuo los logs de los dos Servicios en un único flujo agregado.
6. WHERE la máquina del Desarrollador dispone del ejecutable `make`, THE Entorno_Local SHALL proveer un `Makefile` en la raíz del repositorio que ofrece atajos equivalentes a los Comandos_Operativos anteriores.

### Requirement 11: Reproducibilidad mediante pinning de imágenes

**User Story:** Como Desarrollador, quiero que la imagen efectiva de cada Servicio sea la misma en todas las máquinas del equipo, para evitar diferencias de comportamiento entre entornos por versiones no controladas.

#### Acceptance Criteria

1. THE Entorno_Local SHALL declarar la imagen del Servicio_Postgres pinneada como `postgres:16-alpine`.
2. THE Entorno_Local SHALL declarar la imagen base del Servicio_Backend pinneada como `eclipse-temurin:21-jdk-alpine`.
3. THE Entorno_Local SHALL prohibir el uso del tag `latest` en la definición del compose y en los Dockerfiles de desarrollo.

### Requirement 12: Inicialización del Servicio_Postgres y aplicación de Migraciones_Flyway

**User Story:** Como Desarrollador, quiero que la base de datos y su esquema queden listos para uso automáticamente tras el primer arranque, para no tener que ejecutar scripts de inicialización manualmente.

#### Acceptance Criteria

1. THE Entorno_Local SHALL montar el directorio `infra/docker/postgres/init/` del repositorio como directorio de inicialización del Servicio_Postgres en modo lectura.
2. WHEN el Servicio_Postgres inicializa su Volumen_Persistente por primera vez, THE Entorno_Local SHALL ejecutar los scripts SQL presentes en el directorio de inicialización en orden lexicográfico.
3. THE Entorno_Local SHALL proveer un script de inicialización que habilita las extensiones `uuid-ossp`, `pg_trgm` y `pgcrypto` en la base de datos configurada.
4. WHEN el Servicio_Backend arranca y su Healthcheck aún no ha respondido, THE Entorno_Local SHALL permitir que Spring Boot ejecute todas las Migraciones_Flyway pendientes contra el Servicio_Postgres antes de que el Servicio_Backend reporte estado saludable.
5. IF una Migración_Flyway falla al aplicarse, THEN THE Entorno_Local SHALL propagar el fallo a través del estado del Servicio_Backend, dejándolo en estado no saludable, sin bloquear al Servicio_Postgres.

### Requirement 13: Cache persistente de dependencias de build

**User Story:** Como Desarrollador, quiero que las dependencias descargadas por Maven persistan entre reinicios del contenedor, para no re-descargar dependencias en cada arranque.

#### Acceptance Criteria

1. THE Entorno_Local SHALL montar el directorio `~/.m2` del Servicio_Backend en un Volumen_Persistente nombrado.
2. WHEN el Desarrollador ejecuta el Comando_Operativo de detención sin borrado de volúmenes, THE Entorno_Local SHALL preservar el Volumen_Persistente de cache de dependencias.
3. WHEN el Desarrollador ejecuta el Comando_Operativo de Reset, THE Entorno_Local SHALL eliminar el Volumen_Persistente de cache de dependencias junto con el Volumen_Persistente de datos del Servicio_Postgres.

### Requirement 14: Endpoint de salud del Servicio_Backend

**User Story:** Como Desarrollador, quiero un endpoint de salud del Servicio_Backend consultable desde el host, para verificar de forma automatizada si el backend está listo antes de ejecutar pruebas contra la API.

#### Acceptance Criteria

1. THE Entorno_Local SHALL exponer el endpoint HTTP `GET /actuator/health` del Servicio_Backend a través del Puerto_Publicado documentado en la Plantilla_Env.
2. WHEN el Servicio_Backend está listo para atender peticiones, THE Entorno_Local SHALL responder con el código de estado HTTP 200 sobre el endpoint `GET /actuator/health`.
3. WHEN el Servicio_Backend aún no ha completado su arranque o alguno de sus componentes reporta fallo, THE Entorno_Local SHALL responder con un código de estado HTTP distinto de 200 sobre el endpoint `GET /actuator/health`.

### Requirement 15: API HTTP alcanzable desde Clientes_HTTP_Locales

**User Story:** Como Desarrollador, quiero consumir la API del Servicio_Backend desde herramientas locales (Postman, `curl`, HTTPie, IntelliJ HTTP Client) sin depender de un frontend, para validar manualmente el comportamiento del backend durante el desarrollo.

#### Acceptance Criteria

1. THE Entorno_Local SHALL publicar el puerto HTTP del Servicio_Backend en un Puerto_Publicado del host configurable a través de la Plantilla_Env, con valor por defecto 8080.
2. WHEN un Cliente_HTTP_Local ejecutándose en el host emite una petición HTTP hacia `http://localhost:${BACKEND_HOST_PORT}`, THE Entorno_Local SHALL enrutar la petición al Servicio_Backend sin requerir configuración adicional de proxy ni de red.
3. THE Entorno_Local SHALL exponer al host la respuesta del endpoint `GET /actuator/health` en formato JSON de acuerdo al contrato estándar de Spring Boot Actuator (campo `status` con valor `UP` cuando todos los componentes reportan salud).
4. THE Entorno_Local SHALL versionar una Coleccion_Postman de arranque con, al menos, la petición `GET /actuator/health` documentada, ubicada bajo `docs/postman/` en formato Postman Collection v2.1 y con variables de entorno que referencian `BACKEND_HOST_PORT` para portabilidad entre máquinas.
5. THE Entorno_Local SHALL documentar el flujo de importación de la Coleccion_Postman en Postman u otras herramientas compatibles (Bruno, Insomnia) como parte de la guía de onboarding.

### Requirement 16: Detección de conflictos de Puertos_Publicados

**User Story:** Como Desarrollador, quiero que el arranque del Entorno_Local falle con un mensaje claro si algún Puerto_Publicado ya está ocupado por otro proceso del host, para reasignar el puerto sin diagnosticar a ciegas.

#### Acceptance Criteria

1. IF un Puerto_Publicado configurado en el Archivo_Env está ocupado por otro proceso del host en el momento del arranque, THEN THE Entorno_Local SHALL abortar el arranque del Servicio afectado con un mensaje de error que identifica el puerto en conflicto.
2. WHEN el arranque de un Servicio falla por conflicto de Puerto_Publicado, THE Entorno_Local SHALL mantener a los Servicios cuyos Puertos_Publicados no están en conflicto sin verse afectados en su ciclo de vida.
3. WHEN el Desarrollador modifica el Puerto_Publicado en conflicto en el Archivo_Env y re-invoca el Comando_Operativo de arranque, THE Entorno_Local SHALL retomar el arranque del Servicio afectado usando el nuevo Puerto_Publicado.

### Requirement 17: Documentación de onboarding

**User Story:** Como Desarrollador que se suma por primera vez al proyecto, quiero una guía de onboarding que enumere prerequisitos, pasos de arranque, uso de Postman y solución de problemas comunes, para levantar el Entorno_Local sin asistencia humana adicional.

#### Acceptance Criteria

1. THE Entorno_Local SHALL versionar una guía de onboarding en el repositorio que documenta los prerequisitos de software en el host (Docker Engine, Docker Compose V2, y opcionalmente `make`, un IDE con soporte JDWP y un Cliente_HTTP_Local como Postman).
2. THE Entorno_Local SHALL documentar en la guía de onboarding el Comando_Operativo de arranque, el Comando_Operativo de detención, el Comando_Operativo de Reset y el Comando_Operativo de logs.
3. THE Entorno_Local SHALL documentar en la guía de onboarding el procedimiento para instanciar el Archivo_Env a partir de la Plantilla_Env y para definir el valor de `POSTGRES_PASSWORD`.
4. THE Entorno_Local SHALL documentar en la guía de onboarding las URLs y Puertos_Publicados por defecto del Servicio_Backend y del Servicio_Postgres, así como el Puerto_Publicado del JDWP para adjuntar el debugger.
5. THE Entorno_Local SHALL documentar en la guía de onboarding el procedimiento para importar la Coleccion_Postman y para lanzar la petición `GET /actuator/health` como prueba de humo del entorno.
6. THE Entorno_Local SHALL documentar en la guía de onboarding el procedimiento a seguir ante los escenarios de error identificados como conocidos por el feature (puerto ocupado, `POSTGRES_PASSWORD` no definida, Docker Engine no arrancado, `pom.xml` modificado, fallo de Migración_Flyway).

### Requirement 18: Registro de decisiones arquitectónicas (ADR) asociadas al feature

**User Story:** Como responsable técnico, quiero que las decisiones arquitectónicas del Entorno_Local queden registradas como ADRs en el repositorio, para que los sucesores del equipo comprendan el contexto y las alternativas evaluadas.

#### Acceptance Criteria

1. THE Entorno_Local SHALL versionar un ADR que documenta la elección de PostgreSQL como base de datos del entorno, incluyendo contexto, alternativas evaluadas, decisión y consecuencias.
2. THE Entorno_Local SHALL versionar un ADR que documenta la elección de Docker Compose como orquestador del entorno de desarrollo local, incluyendo contexto, alternativas evaluadas, decisión y consecuencias.
3. THE Entorno_Local SHALL versionar un ADR que documenta la estrategia de Bind_Mount para código fuente del backend combinada con Volumen_Persistente nombrado para el cache de Maven, incluyendo contexto, alternativas evaluadas, decisión y consecuencias.
4. THE Entorno_Local SHALL versionar un ADR que documenta la decisión de posponer el Servicio de frontend a un spec futuro y de validar el backend contra Clientes_HTTP_Locales (Postman) durante esta iteración, incluyendo contexto, alternativas evaluadas, decisión y consecuencias.
5. THE Entorno_Local SHALL almacenar los ADRs bajo el directorio `docs/adr/` del repositorio con nombres de archivo secuencialmente numerados.
