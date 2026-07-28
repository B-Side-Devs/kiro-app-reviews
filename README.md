# Reviews


**Reviews** es una plataforma SaaS diseñada para que desarrolladores, freelancers, agencias y equipos de producto puedan revisar aplicaciones web de una forma mucho más eficiente que utilizando capturas de pantalla, documentos o largos intercambios de mensajes.

En lugar de explicar un problema con texto o imágenes aisladas, Reviews permite **grabar una sesión completa de navegación**, agregar anotaciones visuales directamente sobre la interfaz y compartir el resultado como una review interactiva.


# Reviews

> **Deja de explicar bugs. Empieza a mostrarlos.**

**Reviews** es una plataforma SaaS pensada para desarrolladores, freelancers, agencias y equipos de producto que necesitan revisar aplicaciones web sin perder tiempo en capturas desactualizadas, documentos interminables o hilos de mensajes que nadie vuelve a leer.

La idea es simple: en vez de describir un problema con palabras, **se graba la sesión completa de navegación**, se anota directamente sobre la interfaz y se comparte todo como una review interactiva que cualquiera puede reproducir.

> **Acelera el proceso de revisión de aplicaciones web con sesiones visuales, colaborativas y reproducibles.**

---

## Índice

- [El problema](#el-problema)
- [La solución](#la-solución)
- [¿Para quién está pensado?](#para-quién-está-pensado)
- [Características principales](#características-principales)
- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Estado del proyecto](#estado-del-proyecto)
- [Ejecutar el proyecto en local](#ejecutar-el-proyecto-en-local)
- [Visión](#visión)
- [Licencia](#licencia)

---

## El problema

Revisar software sigue haciéndose, en muchos equipos, casi de la misma forma que hace diez años:

- Capturas de pantalla que quedan desactualizadas apenas se envían.
- Explicaciones ambiguas que dejan más dudas que respuestas.
- El contexto exacto donde ocurrió el error, perdido en el camino.
- Hilos interminables en Slack, Teams o correo.
- Errores casi imposibles de reproducir del otro lado.
- Feedback repartido entre demasiadas herramientas distintas.

Nada de esto es un problema aislado: juntos, alargan cada ciclo de revisión y retrasan lo que realmente importa, que es resolver el problema.

---

## La solución

Reviews reúne todo ese proceso disperso en un único lugar: una sesión interactiva que cuenta la historia completa del problema.

Cada review puede incluir:

- Grabación completa de la navegación, tal como ocurrió.
- Anotaciones visuales directamente sobre la interfaz.
- Comentarios ubicados exactamente donde tienen sentido.
- Capturas de pantalla puntuales.
- Notas de voz *(en el roadmap)*.
- Resúmenes generados mediante IA *(en el roadmap)*.

El resultado no es un reporte más: es una revisión que se entiende de un vistazo, sin necesidad de reconstruir el contexto desde cero.

---

## ¿Para quién está pensado?

Reviews está pensado para cualquier equipo que revise software de forma colaborativa, entre ellos:

- Freelancers
- Agencias digitales
- Equipos de desarrollo
- QA Engineers
- Product Managers
- UX/UI Designers
- Clientes que participan en procesos de aceptación
- Startups y empresas de software

---

## Características principales

- Grabación de sesiones web.
- Reproducción de reviews.
- Anotaciones visuales sobre la aplicación.
- Organización por proyectos.
- Gestión de clientes y workspaces.
- Colaboración entre miembros del equipo.
- Arquitectura modular preparada para escalar.
- API REST desarrollada con Spring Boot.
- Frontend moderno basado en React.

---

## Arquitectura

El proyecto se construye sobre una base pensada para durar, siguiendo principios de:

- Clean Architecture
- Domain-Driven Design (DDD)
- Modular Monolith
- API First
- SOLID
- Preparado para evolucionar hacia microservicios cuando sea necesario

Cada módulo del dominio se desarrolla de forma independiente, lo que reduce el acoplamiento y deja la puerta abierta para extraerlo como servicio autónomo el día que haga falta.

---

## Stack tecnológico

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven

### Frontend
- React
- TypeScript

### Infraestructura
- Docker
- Docker Compose
- Nginx

### Herramientas
- Excalidraw
- rrweb
- OpenAPI / Swagger
- GitHub Actions *(roadmap)*

---

## Estado del proyecto

**En desarrollo activo (MVP).**

El foco actual está puesto en construir los cimientos del producto:

- Gestión de proyectos
- Gestión de reviews
- Grabación de sesiones
- Reproducción de sesiones
- Anotaciones visuales

---

## Ejecutar el proyecto en local

Las instrucciones para levantar el entorno de desarrollo se encuentran en:

\`\`\`text

infra/docker/docker-compose.yml
\`\`\`

---

## Visión

Reviews aspira a convertirse en la plataforma de referencia para revisar aplicaciones web: menos tiempo explicando problemas, más tiempo resolviéndolos, y mejor feedback en cada etapa del ciclo de desarrollo.

> No lo expliques. Compártelo.

---

## Licencia

Este proyecto se encuentra actualmente en desarrollo y no dispone todavía de una licencia pública.