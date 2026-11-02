<!--
Nombre: CLAUDE.md
Descripción: Instrucciones del proyecto InspectIA para Claude Code. Se lee al inicio de cada sesión.
Fecha de creación: 12/10/2026
Actualización: 26/10/2026
Autor: Equipo InspectIA
Versión: 1.12
-->

# InspectIA

App web multiempresa que estima el % de granos de cacao sanos de un lote a partir de 2 o 3 fotos de una muestra (100 a 150 granos) y entrega una categoría probable, una recomendación y un reporte PDF. Proyecto final de Inteligencia Artificial, Universidad Surcolombiana. Docente: Juan Antonio Castro Silva. Equipo, con su usuario de GitHub: Cristhian Sánchez (`DrekOF`), Julián Silva (modelos de IA) y Jhon Perdomo. Los usuarios de Julián y Jhon se agregan cuando entren al repositorio.

## Antes de cada tarea

- La especificación está en `specs/`. Lee lo que la tarea necesite:
  - `specs/fichas.json`: requerimientos RF y RNF con sus criterios de aceptación. Cada tarea implementa una o varias fichas.
  - `specs/decisiones.md`: decisiones (D-xx) y pendientes (P-xx).
  - `specs/modelo-datos.md`: tablas, relaciones y decisiones de diseño del modelo de datos. Las tablas salen de `specs/entidades/*.json` (formato en su README).
  - `specs/arquitectura.md`: piezas, organización del servidor y flujo del análisis.
  - `specs/requisitos-docente.md`: pautas del docente (RP-01 a RP-25). Son obligatorias.
- Si una ficha es ambigua, choca con otra o con una decisión, pregunta antes de asumir. No cambies decisiones por tu cuenta.

## Forma de trabajo

- Una rama y un PR por requerimiento, por ejemplo `feature/RF-04-registrar-lote`. Commits en español que citen el código: `RF-04: valida que el código del lote no se repita`.
- Primero las pruebas de los criterios de aceptación de la ficha; después el código.
- Cada prueba dice qué cubre, porque `docs/casos-de-prueba.md` (RP-13) se arma desde sus marcas con `generador/casos_de_prueba.py` (ver Comandos). Después de agregar o cambiar pruebas, vuelve a armarlo:
  - `«<código> · C<n> · …»` si cubre un criterio de `specs/fichas.json`, por ejemplo `@DisplayName("RF-02 · C4 · dígito de verificación inválido")`. En Python va en `@pytest.mark.caso("…")`.
  - Solo `«<código> · …»` si no cubre un criterio, por ejemplo `«RP-15 · …»`.
  - Los criterios nuevos de una ficha van al final de su lista, para que no cambie el número de los que ya tienen pruebas.
  - Un criterio que deja de aplicar no se borra: se marca «Eliminado (D-xx): …», con la decisión que lo quitó, y conserva su número.
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
- Lo que el JSON no puede declarar de una pantalla generada va a mano en `frontend/src/extensiones.js`, como el panel de administradores de una empresa. Lo generado no importa código escrito a mano.
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
- No agregar a Claude como coautor (`Co-Authored-By`) ni la línea «Generated with Claude Code» en commits ni PR: la autoría es de los integrantes. `.claude/settings.json` lo desactiva para todo el equipo.
- No usar las anotaciones de Lombok que generan constructores (`@RequiredArgsConstructor`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`, `@Data` ni `@Value`). Con el JDK 25, al recompilar sin `clean`, esos constructores quedan sin la llamada a `super()` y la JVM rechaza la clase (`VerifyError`). Escribe el constructor; Lombok queda para getters, setters y `@Slf4j`. `VerificacionDeClasesTest` lo revisa.

## Comandos

Desde la raíz del repositorio. Las versiones y cómo instalarlas están en `README.md`.

- Base de datos local (PostgreSQL 18 en Docker; Docker Desktop debe estar abierto):
  - Primera vez: `cp infra/.env.example infra/.env` y escribe una contraseña en `POSTGRES_PASSWORD` y la clave de los tokens en `INSPECTIA_JWT_CLAVE` (el comando para crearla está en el archivo). Si el 5432 está ocupado o reservado, cambia `POSTGRES_PORT` (ver README).
  - Encender: `docker compose -f infra/docker-compose.yml up -d`
  - Estado: `docker compose -f infra/docker-compose.yml ps`
  - Consola SQL: `docker compose -f infra/docker-compose.yml exec postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'`
  - Apagar: `docker compose -f infra/docker-compose.yml down`. Los datos quedan en el volumen; `down -v` los borra.
  - Validar el archivo: `docker compose -f infra/docker-compose.yml config`
- Backend (desde `backend/`; en PowerShell también sirve `./mvnw`):
  - Todas las pruebas: `./mvnw verify`. Docker Desktop debe estar abierto: las de integración usan PostgreSQL 18 en Testcontainers.
  - Solo las unitarias: `./mvnw test`
  - Una prueba de integración: `./mvnw verify -Dit.test=RegistroEmpresaIT`
  - Arrancar contra la base local: `./mvnw spring-boot:run`. Lee `infra/.env` (sin `INSPECTIA_JWT_CLAVE` no arranca), Flyway crea las tablas y la documentación OpenAPI queda en http://localhost:8080/swagger-ui.html. Para usar la API ahí, inicia sesión con `POST /api/v1/sesion` y pega el token en «Authorize» (ver `infra/README.md`).
- Primer administrador de la plataforma (base encendida, backend arrancado una vez y los datos en `infra/.env`; ver `infra/README.md`):
  - Windows: `powershell -ExecutionPolicy Bypass -File infra\bd\crear-admin-plataforma.ps1`
  - Linux, macOS o Git Bash: `sh infra/bd/crear-admin-plataforma.sh`
- Restablecer la contraseña del administrador de la plataforma (base encendida; pide la contraseña nueva por teclado y, si no está en `infra/.env`, el correo; ver `infra/README.md`):
  - Windows: `powershell -ExecutionPolicy Bypass -File infra\bd\restablecer-admin-plataforma.ps1`
  - Linux, macOS, Git Bash o el servidor: `sh infra/bd/restablecer-admin-plataforma.sh`
- Generador (la primera vez, crea su entorno como dice `generador/README.md`; en Linux o macOS, `generador/.venv/bin/python`):
  - Revisar los JSON: `generador/.venv/Scripts/python generador/generar.py validar`
  - Generar: `generador/.venv/Scripts/python generador/generar.py generar`
  - Comprobar que lo generado está al día y nadie lo editó: `generador/.venv/Scripts/python generador/generar.py comprobar`
  - Pruebas: `cd generador` y `.venv/Scripts/python -m pytest`
- Casos de prueba (RP-13): `generador/.venv/Scripts/python generador/casos_de_prueba.py generar`, o `comprobar` para revisar que el documento esté al día. `comprobar` también falla si una ficha está «Implementado» y tiene criterios sin prueba (D-29).
- Frontend (desde `frontend/`, con Node 24.22.0, la de `frontend/.node-version`; la primera vez, `npm ci`; ver `frontend/README.md`):
  - Arrancar: `npm run dev` abre http://localhost:5173 y pasa `/api` al backend de http://localhost:8080; si corre en otro puerto, define `INSPECTIA_API_URL`.
  - Pruebas unitarias y de componentes: `npm test` (API simulada con MSW, sin backend). ESLint: `npm run lint`. Mutación: `npm run test:mutacion`.
  - De punta a punta: `npm run e2e:playwright` y `npm run e2e:selenium`, con Docker Desktop abierto; la primera vez, `npx playwright install chromium firefox webkit`. Cada corrida enciende su entorno aislado (base en el proyecto `inspectia-e2e`, backend compilado en una copia y la página compilada, sin `infra/.env`) y lo borra al terminar. `npm run e2e:entorno` lo deja encendido; con `INSPECTIA_E2E_URL` y la dirección que muestra, las pruebas lo usan.
  - En Windows 11 25H2 el Firefox de Playwright no abre: `INSPECTIA_E2E_SIN_FIREFOX=1` lo salta (en PowerShell, `$env:INSPECTIA_E2E_SIN_FIREFOX = '1'`).
  - Demostración de otra temática (RP-23), con Docker Desktop abierto y el entorno del generador creado: `npm run demo:otra-tematica` genera producto en una copia (`frontend/.demostracion`, en `.gitignore`), la enciende en el proyecto `inspectia-demo` y Ctrl+C la apaga y borra su base. Con `-- --entidad ../generador/pruebas/ejemplos/cafe.json`, otra entidad, como el café; con `-- --con-pruebas`, antes corre sus pruebas generadas. Las opciones van después de `--`. Guía para la sustentación: `docs/demostracion-otra-tematica.md`.
  - Si una prueba de Selenium falla, su captura y la consola del navegador quedan en `frontend/reports/selenium/`.
- Servicio de IA: se agrega aquí cuando se cree el proyecto.
