<!--
Nombre: README.md (backend)
Descripción: Qué va en la carpeta backend/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# backend

Servidor de InspectIA: API REST con JSON, hecha con Java 25, Spring Boot 4.1, JPA, Lombok y PostgreSQL. Se compila con el wrapper de Maven (`mvnw`). Es un solo programa dividido en módulos, sin microservicios (D-21):

| Módulo | Organización | Qué hace |
|---|---|---|
| `analisis` | Hexagonal | Reglas del lote en `dominio/`, sin Spring: % de granos sanos, categoría y sugerencia. El caso de uso `AnalizarLote` usa el servicio de IA, S3, el PDF y la base de datos solo a través de puertos (interfaces). Mientras llega el modelo, `ClasificadorDeGranos` usa un clasificador falso. |
| `empresa`, `usuario`, `rango` | Capas | CRUD **generados por `generador/`: no se editan a mano** (RP-23). |
| `estadistica` | Capas | Estadísticas y métricas del modelo, hechas a mano (RF-16 y RF-17). |
| `comun` | — | Seguridad (JWT y roles), i18n en español e inglés y manejo de errores. |

También van aquí la documentación OpenAPI (springdoc), el JavaDoc y las pruebas con JUnit, Spring Test, Mockito y Testcontainers (RP-12 a RP-14).

La imagen de Docker se ejecuta sobre `eclipse-temurin:25-jre-noble`, que existe para amd64 y arm64.

Todavía está vacía: el proyecto se crea en la tarea del backend.
