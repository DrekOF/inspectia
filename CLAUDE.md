<!--
Nombre: CLAUDE.md
Descripción: Instrucciones del proyecto InspectIA para Claude Code. Se lee al inicio de cada sesión.
Fecha de creación: 12/10/2026
Actualización: 13/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# InspectIA

App web multiempresa que estima el % de granos de cacao sanos de un lote a partir de 2 o 3 fotos de una muestra (100 a 150 granos) y entrega una categoría probable, una recomendación y un reporte PDF. Proyecto final de Inteligencia Artificial, Universidad Surcolombiana. Docente: Juan Antonio Castro Silva. Equipo, con su usuario de GitHub: Cristhian Sánchez (`DrekOF`), Julián Silvas (modelos de IA) y Jhon Perdomo. Los usuarios de Julián y Jhon se agregan cuando entren al repositorio.

## Antes de cada tarea

- La especificación está en `specs/`. Lee lo que la tarea necesite:
  - `specs/fichas.json`: requerimientos RF y RNF con sus criterios de aceptación. Cada tarea implementa una o varias fichas.
  - `specs/decisiones.md`: decisiones D-01 a D-23 y pendientes.
  - `specs/arquitectura.md`: piezas, organización del servidor y flujo del análisis.
  - `specs/requisitos-docente.md`: pautas del docente (RP-01 a RP-25). Son obligatorias.
- Si una ficha es ambigua, choca con otra o con una decisión, pregunta antes de asumir. No cambies decisiones por tu cuenta.

## Forma de trabajo

- Una rama y un PR por requerimiento, por ejemplo `feature/RF-04-registrar-lote`. Commits en español que citen el código: `RF-04: valida que el código del lote no se repita`.
- Primero las pruebas de los criterios de aceptación de la ficha; después el código.
- Pasos pequeños: al terminar cada tarea, muestra qué cambió y espera confirmación antes de la siguiente.
- Al terminar cada tarea, agrega una entrada en `docs/bitacora.md`: fecha, quién, qué se hizo, por qué y cómo se pidió. Es el registro paso a paso que exige el docente (RP-24). Para saber quién trabaja, busca `git config user.name` en la línea del equipo; si no está, pregunta.

## Arquitectura (D-21)

- Monorepo: `frontend/` (React + Material UI), `backend/` (Spring Boot), `ia-servicio/` (Python, OpenCV y ONNX), `generador/` (Python), `specs/`, `infra/` (Docker Compose, GitHub Actions y scripts de base de datos) y `docs/`.
- El backend es un solo programa dividido en módulos:
  - `analisis` es hexagonal. Las reglas (% de granos sanos, categoría y sugerencia) van en `dominio/`, sin depender de Spring. El caso de uso `AnalizarLote` habla con el servicio de IA, S3, el PDF y la base de datos solo a través de interfaces (puertos).
  - `empresa`, `usuario` y `rango` van por capas y los genera `generador/`.
  - `estadistica` va por capas, a mano. `comun` tiene seguridad (JWT y roles), i18n y manejo de errores.
- Mientras Julián entrega el modelo, el puerto `ClasificadorDeGranos` usa un clasificador falso.
- Sin microservicios ni ECS: todo corre con Docker Compose en una sola instancia EC2 (D-16).
- Nombres del dominio en español y sin tildes en el código (`Lote`, `Grano`, `Analisis`).
- Usa versiones estables actuales (LTS cuando exista) y anótalas en el README.

## Generador: regla clave (RP-23)

- Los CRUD (tablas, backend y pantallas) no se escriben a mano: salen de `generador/` a partir de `specs/entidades/*.json`.
- Para cambiar un CRUD, cambia el JSON o la plantilla y vuelve a generar. Nunca edites a mano el código generado.
- La meta es poder cambiar de temática (otro producto en lugar de cacao) cambiando solo los JSON.

## Reglas del docente para todo el código

- Header al inicio de cada artefacto (RP-10): nombre, descripción, fecha de creación, actualización, autor y versión. Al modificar un archivo, actualiza su header (versión y fecha de actualización).
- Backend: Java, Spring Boot, JPA, Lombok y PostgreSQL; validaciones y seguridad; JavaDoc, comentarios y OpenAPI; intercambio en JSON; i18n en español e inglés.
- Frontend: React y Material UI; formularios con Formik y Yup; responsive, accesible, con tokens de colores y temas; JSDoc y comentarios; i18n.
- Pruebas: JUnit, Spring Test y Mockito en el backend; Playwright y Selenium en el frontend. Tipos: unitarias, de integración, de sistema, de seguridad, de mutación, de estrés y de límite. Cada criterio de aceptación de `specs/fichas.json` tiene al menos un caso de prueba (RP-13).
- Despliegue: scripts de base de datos, de compilación y de prueba; GitHub Actions despliega si las pruebas pasan y crea un issue y avisa si fallan (RP-15 a RP-19).

## No hacer

- No subir el dataset ni fotos pesadas al repositorio: pesa 4,9 GB y tiene licencia CC BY-NC 4.0. Cítalo siempre (RNF-08).
- No guardar claves ni contraseñas en el repositorio. AWS se conecta por OIDC.
- No reentrenar modelos desde la app (D-03).
- No fijar el puerto de la base (5432) en el código ni en la configuración: el backend lo lee de `POSTGRES_PORT`, porque cambia en cada equipo (en algunos Windows el 5432 está reservado).

## Comandos

Desde la raíz del repositorio. Las versiones y cómo instalarlas están en `README.md`.

- Base de datos local (PostgreSQL 18 en Docker; Docker Desktop debe estar abierto):
  - Primera vez: `cp infra/.env.example infra/.env` y escribe una contraseña en `POSTGRES_PASSWORD`. Si el 5432 está ocupado o reservado, cambia `POSTGRES_PORT` (ver README).
  - Encender: `docker compose -f infra/docker-compose.yml up -d`
  - Estado: `docker compose -f infra/docker-compose.yml ps`
  - Consola SQL: `docker compose -f infra/docker-compose.yml exec postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'`
  - Apagar: `docker compose -f infra/docker-compose.yml down`. Los datos quedan en el volumen; `down -v` los borra.
  - Validar el archivo: `docker compose -f infra/docker-compose.yml config`
- Backend, frontend, servicio de IA y generador: se agregan aquí cuando se cree cada proyecto.
